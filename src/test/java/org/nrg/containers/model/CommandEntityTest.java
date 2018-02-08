package org.nrg.containers.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.CustomTypeSafeMatcher;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.nrg.containers.config.CommandTestConfig;
import org.nrg.containers.model.command.auto.Command;
import org.nrg.containers.model.command.auto.Command.CommandInput;
import org.nrg.containers.model.command.auto.Command.CommandMount;
import org.nrg.containers.model.command.auto.Command.CommandOutput;
import org.nrg.containers.model.command.auto.Command.CommandWrapper;
import org.nrg.containers.model.command.auto.Command.CommandWrapperDerivedInput;
import org.nrg.containers.model.command.auto.Command.CommandWrapperExternalInput;
import org.nrg.containers.model.command.auto.Command.CommandWrapperOutput;
import org.nrg.containers.model.command.entity.CommandEntity;
import org.nrg.containers.model.command.entity.CommandInputEntity;
import org.nrg.containers.model.command.entity.CommandWrapperEntity;
import org.nrg.containers.services.CommandEntityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.nio.file.Paths;
import java.util.List;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@Transactional
@ContextConfiguration(classes = CommandTestConfig.class)
public class CommandEntityTest {

    private Command COMMAND;
    private CommandEntity COMMAND_ENTITY;

    @Autowired private ObjectMapper mapper;
    @Autowired private CommandEntityService commandEntityService;

    @Rule public ExpectedException expectedException = ExpectedException.none();

    @Before
    public void setup() throws Exception {
        final String outputMountName = "out";
        final CommandMount mountIn = CommandMount.create("in", false, "/input");
        final CommandMount mountOut = CommandMount.create(outputMountName, true, "/output");

        final String stringInputName = "foo";
        final CommandInput stringInput = CommandInput.builder()
                .name(stringInputName)
                .description("A foo that bars")
                .required(false)
                .defaultValue("bar")
                .commandLineFlag("--flag")
                .commandLineSeparator("=")
                .build();
        final CommandInput coolInput = CommandInput.builder()
                .name("my_cool_input")
                .description("A boolean value")
                .type("boolean")
                .required(true)
                .trueValue("-b")
                .falseValue("")
                .build();

        final String commandOutputName = "the_output";
        final CommandOutput commandOutput = CommandOutput.builder()
                .name(commandOutputName)
                .description("It's the output")
                .mount(outputMountName)
                .path("relative/path/to/dir")
                .build();

        final String externalInputName = "session";
        final CommandWrapperExternalInput externalInput = CommandWrapperExternalInput.builder()
                .name(externalInputName)
                .type("Session")
                .build();

        final String derivedInputName = "label";
        final String xnatObjectProperty = "label";
        final CommandWrapperDerivedInput derivedInput = CommandWrapperDerivedInput.builder()
                .name(derivedInputName)
                .type("string")
                .derivedFromWrapperInput(externalInputName)
                .derivedFromXnatObjectProperty(xnatObjectProperty)
                .providesValueForCommandInput(stringInputName)
                .build();

        final String outputHandlerName = "output-handler-name";
        final String outputHandlerLabel = "a_label";
        final CommandWrapperOutput outputHandler = CommandWrapperOutput.create(outputHandlerName,
                commandOutputName, externalInputName, "Resource", outputHandlerLabel);

        final String commandWrapperName = "wrappername";
        final String commandWrapperDesc = "the wrapper description";
        final CommandWrapper commandWrapper = CommandWrapper.builder()
                .name(commandWrapperName)
                .description(commandWrapperDesc)
                .addExternalInput(externalInput)
                .addDerivedInput(derivedInput)
                .addOutputHandler(outputHandler)
                .build();

        COMMAND = Command.builder()
                .name("docker_image_command")
                .description("Docker Image command for the test")
                .image("abc123")
                .type("docker")
                .infoUrl("http://abc.xyz")
                .addEnvironmentVariable("foo", "bar")
                .commandLine("cmd #foo# #my_cool_input#")
                .reserveMemory(4000L)
                .limitMemory(8000L)
                .limitCpu(0.5D)
                .addMount(mountIn)
                .addMount(mountOut)
                .addInput(coolInput)
                .addInput(stringInput)
                .addOutput(commandOutput)
                .addPort("22", "2222")
                .addCommandWrapper(commandWrapper)
                .build();

        COMMAND_ENTITY = CommandEntity.fromPojo(COMMAND);

    }

    @Test
    public void testSpringConfiguration() {
        assertThat(commandEntityService, not(nullValue()));
    }

    @Test
    public void testSerializeDeserializeCommand() throws Exception {
        assertThat(mapper.readValue(mapper.writeValueAsString(COMMAND), Command.class), is(COMMAND));
    }

    @Test
    @DirtiesContext
    public void testPersistCommandWithWrapper() throws Exception {
        final CommandEntity created = commandEntityService.create(COMMAND_ENTITY);

        TestTransaction.flagForCommit();
        TestTransaction.end();
        TestTransaction.start();

        final CommandEntity retrievedCommandEntity = commandEntityService.retrieve(created.getId());

        assertThat(retrievedCommandEntity, is(created));
        assertThat(Command.create(created).validate(), is(Matchers.<String>emptyIterable()));

        final List<CommandWrapperEntity> commandWrappers = retrievedCommandEntity.getCommandWrapperEntities();
        assertThat(commandWrappers, hasSize(1));

        final CommandWrapperEntity commandWrapperEntity = commandWrappers.get(0);
        assertThat(commandWrapperEntity.getId(), not(0L));
        assertThat(commandWrapperEntity.getCommandEntity(), is(created));
    }

    @Test
    @DirtiesContext
    public void testDeleteCommandWithWrapper() throws Exception {

        final CommandEntity created = commandEntityService.create(COMMAND_ENTITY);

        TestTransaction.flagForCommit();
        TestTransaction.end();
        TestTransaction.start();

        commandEntityService.delete(created);

        TestTransaction.flagForCommit();
        TestTransaction.end();
        TestTransaction.start();

        assertThat(commandEntityService.retrieve(created.getId()), is(nullValue()));
    }

    @Test
    @DirtiesContext
    public void testRetrieveCommandWrapper() throws Exception {

        final CommandEntity created = commandEntityService.create(COMMAND_ENTITY);

        TestTransaction.flagForCommit();
        TestTransaction.end();
        TestTransaction.start();

        final CommandWrapperEntity createdWrapper = created.getCommandWrapperEntities().get(0);
        final long wrapperId = createdWrapper.getId();
        assertThat(commandEntityService.retrieveWrapper(wrapperId), is(createdWrapper));

        assertThat(Command.create(created).validate(), is(Matchers.<String>emptyIterable()));
    }

    @Test
    @DirtiesContext
    public void testAddCommandWrapper() throws Exception {

        final CommandWrapperEntity toAdd = COMMAND_ENTITY.getCommandWrapperEntities().get(0);
        COMMAND_ENTITY.setCommandWrapperEntities(null);

        final CommandEntity created = commandEntityService.create(COMMAND_ENTITY);

        TestTransaction.flagForCommit();
        TestTransaction.end();
        TestTransaction.start();

        final CommandWrapperEntity added = commandEntityService.addWrapper(created, toAdd);

        TestTransaction.flagForCommit();
        TestTransaction.end();
        TestTransaction.start();

        final CommandEntity retrieved = commandEntityService.get(COMMAND_ENTITY.getId());
        assertThat(retrieved.getCommandWrapperEntities().get(0), is(added));

        assertThat(Command.create(retrieved).validate(), is(Matchers.<String>emptyIterable()));
    }

    @Test
    @DirtiesContext
    public void testUpdateCommandWrapperDescription() throws Exception {

        final CommandEntity created = commandEntityService.create(COMMAND_ENTITY);

        TestTransaction.flagForCommit();
        TestTransaction.end();
        TestTransaction.start();

        final CommandWrapperEntity createdWrapper = created.getCommandWrapperEntities().get(0);

        final String newDescription = "This is probably a new description, right?";
        createdWrapper.setDescription(newDescription);

        commandEntityService.update(created);
        TestTransaction.flagForCommit();
        TestTransaction.end();
        TestTransaction.start();

        final CommandEntity retrieved = commandEntityService.get(created.getId());
        final CommandWrapperEntity retrievedWrapper = retrieved.getCommandWrapperEntities().get(0);

        assertThat(retrievedWrapper.getDescription(), is(newDescription));
        assertThat(Command.create(retrieved).validate(), is(Matchers.<String>emptyIterable()));
    }

    @Test
    @DirtiesContext
    public void testUpdateAddInput() throws Exception {

        final CommandEntity created = commandEntityService.create(COMMAND_ENTITY);

        TestTransaction.flagForCommit();
        TestTransaction.end();
        TestTransaction.start();

        final CommandInput inputToAdd = CommandInput.builder()
                .name("this is new")
                .description("A new input that didn't exist before")
                .commandLineFlag("--flag")
                .commandLineSeparator("=")
                .defaultValue("yes")
                .build();
        created.addInput(CommandInputEntity.fromPojo(inputToAdd));

        commandEntityService.update(created);
        TestTransaction.flagForCommit();
        TestTransaction.end();
        TestTransaction.start();

        final CommandEntity retrieved = commandEntityService.get(created.getId());

        final Command retrievedPojo = Command.create(retrieved);
        assertThat(inputToAdd, isInIgnoreId(retrievedPojo.inputs()));
        assertThat(retrievedPojo.validate(), is(Matchers.<String>emptyIterable()));
    }

    private Matcher<CommandInput> isInIgnoreId(final List<CommandInput> expected) {
        final String description = "a CommandInput equal to (other than the ID) one of " + expected;
        return new CustomTypeSafeMatcher<CommandInput>(description) {
            @Override
            protected boolean matchesSafely(final CommandInput actual) {
                for (final CommandInput input : expected) {
                    final CommandInput actualWithSameId =
                            actual.toBuilder().id(input.id()).build();
                    if (input.equals(actualWithSameId)) {
                        return true;
                    }
                }
                return false;
            }
        };
    }

    @Test
    @DirtiesContext
    public void testDeleteCommandWrapper() throws Exception {

        final CommandEntity created = commandEntityService.create(COMMAND_ENTITY);

        TestTransaction.flagForCommit();
        TestTransaction.end();
        TestTransaction.start();

        final long wrapperId = created.getCommandWrapperEntities().get(0).getId();
        commandEntityService.deleteWrapper(wrapperId);

        TestTransaction.flagForCommit();
        TestTransaction.end();
        TestTransaction.start();

        assertThat(commandEntityService.retrieveWrapper(wrapperId), is(nullValue()));
    }

    @Test
    @DirtiesContext
    public void testGetCommandsByImage() throws Exception {
        final String fooImage = "xnat/foo:1.2.3";
        final String barImage = "xnat/bar:4.5.6";
        final Command fooImageCommand1 = Command.builder()
                .image(fooImage)
                .name("soahs")
                .version("0")
                .build();
        final Command fooImageCommand2 = Command.builder()
                .image(fooImage)
                .name("asuyfo")
                .version("0")
                .build();
        final Command barImageCommand = Command.builder()
                .image(barImage)
                .name("dosfa")
                .version("0")
                .build();

        final CommandEntity fooImageCommandEntity1 = commandEntityService.create(CommandEntity.fromPojo(fooImageCommand1));
        final CommandEntity fooImageCommandEntity2 = commandEntityService.create(CommandEntity.fromPojo(fooImageCommand2));
        final CommandEntity barImageCommandEntity = commandEntityService.create(CommandEntity.fromPojo(barImageCommand));

        final List<CommandEntity> fooImageCommandsRetrieved = commandEntityService.getByImage(fooImage);
        assertThat(fooImageCommandsRetrieved, hasSize(2));
        assertThat(fooImageCommandsRetrieved, contains(fooImageCommandEntity1, fooImageCommandEntity2));
        assertThat(fooImageCommandsRetrieved, not(contains(barImageCommandEntity)));

        final List<CommandEntity> barImageCommandsRetrieved = commandEntityService.getByImage(barImage);
        assertThat(barImageCommandsRetrieved, hasSize(1));
        assertThat(barImageCommandsRetrieved, not(contains(fooImageCommandEntity1, fooImageCommandEntity2)));
        assertThat(barImageCommandsRetrieved, contains(barImageCommandEntity));
    }

    @Test
    @DirtiesContext
    public void testCreateEcatHeaderDump() throws Exception {
        // A User was attempting to create the command in this resource.
        // Spring didn't tell us why. See CS-70.
        final String dir = Paths.get(ClassLoader.getSystemResource("ecatHeaderDump").toURI()).toString().replace("%20", " ");
        final String commandJsonFile = dir + "/command.json";
        final Command ecatHeaderDump = mapper.readValue(new File(commandJsonFile), Command.class);
        commandEntityService.create(CommandEntity.fromPojo(ecatHeaderDump));
    }

    @Test
    @DirtiesContext
    public void testCreateSetupCommand() throws Exception {
        final Command setupCommand = Command.builder()
                .name("setup")
                .type("docker-setup")
                .image("a-setup-image")
                .build();
        final List<String> errors = setupCommand.validate();
        assertThat(errors, is(Matchers.<String>emptyIterable()));
        final CommandEntity createdSetupCommandEntity = commandEntityService.create(CommandEntity.fromPojo(setupCommand));
    }
}
