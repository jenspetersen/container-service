package org.nrg.containers.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.hibernate.SessionFactory;
import org.mockito.Mockito;
import org.nrg.config.services.ConfigService;
import org.nrg.containers.api.ContainerControlApi;
import org.nrg.containers.api.DockerControlApi;
import org.nrg.containers.daos.ContainerEntityRepository;
import org.nrg.containers.daos.DockerServerEntityRepository;
import org.nrg.containers.events.listeners.DockerContainerEventListener;
import org.nrg.containers.events.listeners.DockerServiceEventListener;
import org.nrg.containers.model.command.entity.CommandEntity;
import org.nrg.containers.model.command.entity.CommandInputEntity;
import org.nrg.containers.model.command.entity.CommandMountEntity;
import org.nrg.containers.model.command.entity.CommandOutputEntity;
import org.nrg.containers.model.command.entity.CommandWrapperDerivedInputEntity;
import org.nrg.containers.model.command.entity.CommandWrapperEntity;
import org.nrg.containers.model.command.entity.CommandWrapperExternalInputEntity;
import org.nrg.containers.model.command.entity.CommandWrapperOutputEntity;
import org.nrg.containers.model.command.entity.DockerCommandEntity;
import org.nrg.containers.model.command.entity.DockerSetupCommandEntity;
import org.nrg.containers.model.container.entity.ContainerEntity;
import org.nrg.containers.model.container.entity.ContainerEntityHistory;
import org.nrg.containers.model.container.entity.ContainerEntityInput;
import org.nrg.containers.model.container.entity.ContainerEntityMount;
import org.nrg.containers.model.container.entity.ContainerEntityOutput;
import org.nrg.containers.model.container.entity.ContainerMountFilesEntity;
import org.nrg.containers.model.server.docker.DockerServerEntity;
import org.nrg.containers.services.CommandLabelService;
import org.nrg.containers.services.CommandResolutionService;
import org.nrg.containers.services.CommandService;
import org.nrg.containers.services.ContainerEntityService;
import org.nrg.containers.services.ContainerFinalizeService;
import org.nrg.containers.services.ContainerService;
import org.nrg.containers.services.DockerHubService;
import org.nrg.containers.services.DockerServerEntityService;
import org.nrg.containers.services.DockerServerService;
import org.nrg.containers.services.DockerService;
import org.nrg.containers.services.SetupCommandService;
import org.nrg.containers.services.impl.CommandLabelServiceImpl;
import org.nrg.containers.services.impl.CommandResolutionServiceImpl;
import org.nrg.containers.services.impl.ContainerFinalizeServiceImpl;
import org.nrg.containers.services.impl.ContainerServiceImpl;
import org.nrg.containers.services.impl.DockerServerServiceImpl;
import org.nrg.containers.services.impl.DockerServiceImpl;
import org.nrg.containers.services.impl.HibernateContainerEntityService;
import org.nrg.containers.services.impl.HibernateDockerServerEntityService;
import org.nrg.containers.services.impl.SetupCommandServiceImpl;
import org.nrg.framework.services.ContextService;
import org.nrg.framework.services.NrgEventService;
import org.nrg.transporter.TransportService;
import org.nrg.transporter.TransportServiceImpl;
import org.nrg.xdat.preferences.SiteConfigPreferences;
import org.nrg.xdat.security.services.PermissionsServiceI;
import org.nrg.xdat.services.AliasTokenService;
import org.nrg.xnat.services.archive.CatalogService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.orm.hibernate4.HibernateTransactionManager;
import org.springframework.orm.hibernate4.LocalSessionFactoryBean;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.ResourceTransactionManager;
import reactor.Environment;
import reactor.bus.EventBus;
import reactor.core.Dispatcher;
import reactor.core.dispatch.RingBufferDispatcher;

import javax.sql.DataSource;
import java.util.Properties;

@Configuration
@EnableTransactionManagement
@Import({CommandConfig.class, HibernateConfig.class, RestApiTestConfig.class})
public class IntegrationTestConfig {
    /*
    Control API and dependencies + Events
     */
    @Bean
    public DockerControlApi dockerControlApi(final DockerServerService dockerServerService,
                                             final CommandLabelService commandLabelService,
                                             final NrgEventService eventService) {
        return new DockerControlApi(dockerServerService, commandLabelService, eventService);
    }

    @Bean
    public DockerServerService dockerServerService(final DockerServerEntityService dockerServerEntityService) {
        return new DockerServerServiceImpl(dockerServerEntityService);
    }

    @Bean
    public DockerServerEntityService dockerServerEntityService() {
        return new HibernateDockerServerEntityService();
    }

    @Bean
    public DockerServerEntityRepository dockerServerEntityRepository() {
        return new DockerServerEntityRepository();
    }

    @Bean
    public Environment env() {
        return Environment.initializeIfEmpty().assignErrorJournal();
    }

    @Bean
    public Dispatcher dispatcher() {
        return new RingBufferDispatcher("dispatch");
    }

    @Bean
    public EventBus eventBus(final Environment env, final Dispatcher dispatcher) {
        return EventBus.create(env, dispatcher);
    }

    @Bean
    public NrgEventService nrgEventService(final EventBus eventBus) {
        return new NrgEventService(eventBus);
    }

    @Bean
    public DockerContainerEventListener containerEventListener(final EventBus eventBus) {
        return new DockerContainerEventListener(eventBus);
    }

    @Bean
    public DockerServiceEventListener serviceEventListener(final EventBus eventBus) {
        return new DockerServiceEventListener(eventBus);
    }

    @Bean
    public CommandLabelService commandLabelService(final ObjectMapper objectMapper) {
        return new CommandLabelServiceImpl(objectMapper);
    }

    /*
    Container launch Service and dependencies
     */
    @Bean
    public ContainerService containerService(final ContainerControlApi containerControlApi,
                                             final ContainerEntityService containerEntityService,
                                             final CommandResolutionService commandResolutionService,
                                             final AliasTokenService aliasTokenService,
                                             final SiteConfigPreferences siteConfigPreferences,
                                             final ContainerFinalizeService containerFinalizeService) {
        return new ContainerServiceImpl(containerControlApi, containerEntityService,
                        commandResolutionService, aliasTokenService, siteConfigPreferences,
                        containerFinalizeService);
    }

    @Bean
    public CommandResolutionService commandResolutionService(final CommandService commandService,
                                                             final ConfigService configService,
                                                             final SiteConfigPreferences siteConfigPreferences,
                                                             final ObjectMapper objectMapper,
                                                             final SetupCommandService setupCommandService) {
        return new CommandResolutionServiceImpl(commandService, configService, siteConfigPreferences, objectMapper, setupCommandService);
    }

    @Bean
    public SetupCommandService setupCommandService(final CommandService commandService,
                                                   final DockerService dockerService) {
        return new SetupCommandServiceImpl(commandService, dockerService);
    }

    @Bean
    public DockerService dockerService(final ContainerControlApi controlApi,
                                       final DockerHubService dockerHubService,
                                       final CommandService commandService,
                                       final DockerServerService dockerServerService,
                                       final CommandLabelService commandLabelService) {
        return new DockerServiceImpl(controlApi, dockerHubService, commandService, dockerServerService, commandLabelService);
    }

    @Bean
    public DockerHubService mockDockerHubService() {
        return Mockito.mock(DockerHubService.class);
    }

    @Bean
    public ContainerFinalizeService containerFinalizeService(final ContainerControlApi containerControlApi,
                                                             final SiteConfigPreferences siteConfigPreferences,
                                                             final TransportService transportService,
                                                             final CatalogService catalogService) {
        return new ContainerFinalizeServiceImpl(containerControlApi, siteConfigPreferences, transportService, catalogService);
    }

    @Bean
    public TransportService transportService() {
        return new TransportServiceImpl();
    }

    @Bean
    public AliasTokenService aliasTokenService() {
        return Mockito.mock(AliasTokenService.class);
    }

    @Bean
    public SiteConfigPreferences siteConfigPreferences() {
        return Mockito.mock(SiteConfigPreferences.class);
    }

    @Bean
    public ConfigService configService() {
        return Mockito.mock(ConfigService.class);
    }

    @Bean
    public PermissionsServiceI permissionsService() {
        return Mockito.mock(PermissionsServiceI.class);
    }

    @Bean
    public CatalogService catalogService() {
        return Mockito.mock(CatalogService.class);
    }

    @Bean
    public ContextService contextService(final ApplicationContext applicationContext) {
        final ContextService contextService = new ContextService();
        contextService.setApplicationContext(applicationContext);
        return contextService;
    }

    /*
    Container entity service and dependencies
     */
    @Bean
    public ContainerEntityService containerEntityService() {
        return new HibernateContainerEntityService();
    }

    @Bean
    public ContainerEntityRepository containerEntityRepository() {
        return new ContainerEntityRepository();
    }

    /*
    Session factory
     */
    @Bean
    public LocalSessionFactoryBean sessionFactory(final DataSource dataSource, @Qualifier("hibernateProperties") final Properties properties) {
        final LocalSessionFactoryBean bean = new LocalSessionFactoryBean();
        bean.setDataSource(dataSource);
        bean.setHibernateProperties(properties);
        bean.setAnnotatedClasses(
                DockerServerEntity.class,
                CommandEntity.class,
                DockerCommandEntity.class,
                DockerSetupCommandEntity.class,
                CommandInputEntity.class,
                CommandOutputEntity.class,
                CommandMountEntity.class,
                CommandWrapperEntity.class,
                CommandWrapperExternalInputEntity.class,
                CommandWrapperDerivedInputEntity.class,
                CommandWrapperOutputEntity.class,
                ContainerEntity.class,
                ContainerEntityHistory.class,
                ContainerEntityInput.class,
                ContainerEntityOutput.class,
                ContainerEntityMount.class,
                ContainerMountFilesEntity.class);

        return bean;
    }

    @Bean
    public ResourceTransactionManager transactionManager(final SessionFactory sessionFactory) throws Exception {
        return new HibernateTransactionManager(sessionFactory);
    }
}
