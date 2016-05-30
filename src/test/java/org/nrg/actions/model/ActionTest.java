package org.nrg.actions.model;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nrg.actions.config.ActionTestConfig;
import org.nrg.actions.model.matcher.Matcher;
import org.nrg.actions.services.ActionService;
import org.nrg.actions.services.CommandService;
import org.nrg.containers.model.DockerImage;
import org.nrg.containers.services.DockerImageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isIn;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

@RunWith(SpringJUnit4ClassRunner.class)
@Transactional
@ContextConfiguration(classes = ActionTestConfig.class)
public class ActionTest {
    private static final String DOCKER_IMAGE_JSON =
            "{\"name\":\"name\", \"repo-tags\":[\"a\", \"b\"], \"image-id\":\"abc123\"," +
                    "\"labels\":{\"foo\":\"bar\"}}";

    private static final String SCAN_MATCHER_JSON =
            "{\"property\":\"type\", \"operator\":\"equals\", \"value\":\"T1|MPRAGE\"}";

    private static final String VARIABLE_0_JSON =
            "{\"name\":\"my_cool_input\", \"description\":\"A boolean value\", " +
                    "\"type\":\"boolean\", \"required\":true," +
                    "\"true-value\":\"-b\", \"false-value\":\"\"," +
                    "\"value\":\"true\"}";
    private static final String VARIABLE_1_JSON =
            "{\"name\":\"my_uncool_input\", \"description\":\"No one loves me :(\", " +
                    "\"type\":\"string\", \"required\":false," +
                    "\"arg-template\":\"--uncool=#value#\"}";
    private static final String VARIABLE_LIST_JSON =
            "[" + VARIABLE_0_JSON + ", " + VARIABLE_1_JSON + "]";
    private static final String ACTION_INPUT_JSON =
            "{\"name\":\"some_identifier\", \"command-variable-name\":\"my_cool_input\", " +
                    "\"root-property\":\"label\", " +
                    "\"required\":true, \"type\":\"string\"," +
                    "\"value\":\"something\"}";

    private static final String COMMAND_MOUNT_IN_JSON =
            "{\"name\":\"in\", \"path\":\"/input\"}";
    private static final String COMMAND_MOUNT_OUT_JSON =
            "{\"name\":\"out\", \"path\":\"/output\"}";
    private static final String ACTION_RESOURCE_STAGED_JSON =
            "{\"name\":\"DICOM\", \"mount\":\"in\", \"id\":10}";
    private static final String ACTION_RESOURCE_CREATED_JSON =
            "{\"name\":\"NIFTI\", \"mount\":\"out\", \"overwrite\":true}";

    private static final String COMMAND_JSON_TEMPLATE =
            "{\"name\":\"docker_image_command\", \"description\":\"Docker Image command for the test\", " +
                    "\"info-url\":\"http://abc.xyz\", \"env\":{\"foo\":\"bar\"}, " +
                    "\"variables\":" + VARIABLE_LIST_JSON + ", " +
                    "\"template\":\"foo\", \"type\":\"docker-image\", " +
                    "\"docker-image\":{\"id\":%d}, " +
                    "\"mounts-in\":[" + COMMAND_MOUNT_IN_JSON + "]," +
                    "\"mounts-out\":[" + COMMAND_MOUNT_OUT_JSON + "]}";

    private static final String ACTION_ROOT_JSON =
            "{\"name\":\"scan\", \"xsiType\":\"xnat:imageScanData\"," +
                    "\"matchers\": [" + SCAN_MATCHER_JSON + "]}";
    private static final String ACTION_JSON_TEMPLATE =
            "{\"name\":\"an_action\", \"description\":\"Yep, it's an action all right\", " +
                    "\"command-id\":%d, " +
                    "\"root\": " + ACTION_ROOT_JSON + "," +
                    "\"inputs\":[" + ACTION_INPUT_JSON + "]," +
                    "\"resources-created\":[" + ACTION_RESOURCE_CREATED_JSON + "]," +
                    "\"resources-staged\":[" + ACTION_RESOURCE_STAGED_JSON + "]}";

    private static final String ACTION_MINIMAL_JSON_TEMPLATE =
            "{\"name\":\"an_action\", \"description\":\"Yep, it's an action all right\", " +
                    "\"command-id\":%d, " +
                    "\"root\": " + ACTION_ROOT_JSON + "}";

    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    private ActionService actionService;

    @Autowired
    private CommandService commandService;

    @Autowired
    private DockerImageService dockerImageService;

    @Test
    public void testDeserializeMatcher() throws Exception {
        final Matcher matcher =
                mapper.readValue(SCAN_MATCHER_JSON, Matcher.class);

        //assertTrue(StringMatcher.class.isAssignableFrom(matcher.getClass()));
        assertEquals("T1|MPRAGE", matcher.getValue());
        assertEquals("equals", matcher.getOperator());
        assertEquals("type", matcher.getProperty());
    }

    @Test
    public void testDeserializeActionInput() throws Exception {

        final String commandJson = String.format(COMMAND_JSON_TEMPLATE, 0);
        final Command command = mapper.readValue(commandJson, Command.class);
        final CommandVariable commandVariable = command.getVariables().get(0);

        final ActionInput actionInput = mapper.readValue(ACTION_INPUT_JSON, ActionInput.class);

        assertEquals("some_identifier", actionInput.getInputName());
        assertEquals(commandVariable.getName(), actionInput.getCommandVariableName());
        assertTrue(actionInput.getRequired());
        assertEquals("string", actionInput.getType());
        assertEquals("something", actionInput.getValue());
        assertEquals("label", actionInput.getRootProperty());
    }

    @Test
    public void testDeserializeActionRoot() throws Exception {
        final ActionRoot root =
                mapper.readValue(ACTION_ROOT_JSON, ActionRoot.class);

        assertEquals("scan", root.getRootName());
        assertEquals("xnat:imageScanData", root.getXsiType());

        assertThat(root.getMatchers(), hasSize(1));
        final Matcher matcher = root.getMatchers().get(0);
        assertThat(matcher.getType(), is(nullValue()));
        assertEquals("type", matcher.getProperty());
        assertEquals("equals", matcher.getOperator());
        assertEquals("T1|MPRAGE", matcher.getValue());
    }

    @Test
    public void testDeserializeActionResources() throws Exception {
        final ActionResource staged =
                mapper.readValue(ACTION_RESOURCE_STAGED_JSON, ActionResource.class);
        final ActionResource created =
                mapper.readValue(ACTION_RESOURCE_CREATED_JSON, ActionResource.class);

        assertEquals("DICOM", staged.getResourceName());
        assertEquals("in", staged.getMountName());
        assertEquals(Integer.valueOf(10), staged.getResourceId());
        assertFalse(staged.getOverwrite());

        assertEquals("NIFTI", created.getResourceName());
        assertEquals("out", created.getMountName());
        assertThat(created.getResourceId(), is(nullValue()));
        assertTrue(created.getOverwrite());
    }

    @Test
    public void testDeserializeAction() throws Exception {
        final ActionRoot root = mapper.readValue(ACTION_ROOT_JSON, ActionRoot.class);
        final ActionInput input = mapper.readValue(ACTION_INPUT_JSON, ActionInput.class);
        final ActionResource created =
                mapper.readValue(ACTION_RESOURCE_CREATED_JSON, ActionResource.class);
        final ActionResource staged =
                mapper.readValue(ACTION_RESOURCE_STAGED_JSON, ActionResource.class);

        final String actionJson = String.format(ACTION_JSON_TEMPLATE, 0);
        final ActionDto actionDto = mapper.readValue(actionJson, ActionDto.class);

        assertEquals("an_action", actionDto.getName());
        assertEquals("Yep, it's an action all right", actionDto.getDescription());
        assertEquals(Long.valueOf(0), actionDto.getCommandId());
        assertEquals(root, actionDto.getRoot());
        assertThat(actionDto.getInputs(), hasSize(1));
        assertEquals(input, actionDto.getInputs().get(0));
        assertEquals(created, actionDto.getResourcesCreated().get(0));
        assertEquals(staged, actionDto.getResourcesStaged().get(0));
    }


    @Test
    public void testCreateMinimalAction() throws Exception {
        final List<CommandVariable> variables =
                mapper.readValue(VARIABLE_LIST_JSON,
                        new TypeReference<List<CommandVariable>>() {});
        final CommandMount mountIn =
                mapper.readValue(COMMAND_MOUNT_IN_JSON, CommandMount.class);
        final CommandMount mountOut =
                mapper.readValue(COMMAND_MOUNT_OUT_JSON, CommandMount.class);
        final String commandJson = String.format(COMMAND_JSON_TEMPLATE, 0);
        final Command command = mapper.readValue(commandJson, Command.class);

        final ActionRoot root = mapper.readValue(ACTION_ROOT_JSON, ActionRoot.class);
        final List<ActionInput> defaultInputs = Lists.newArrayList();
        for (final CommandVariable variable : variables) {
            defaultInputs.add(new ActionInput(variable));
        }

        final ActionResource defaultResourceStaged = new ActionResource(mountIn);
        final ActionResource defaultResourceCreated = new ActionResource(mountOut);

        final String actionJson =
                String.format(ACTION_MINIMAL_JSON_TEMPLATE, command.getId());
        final ActionDto actionDto = mapper.readValue(actionJson, ActionDto.class);

        final Action minimalAction = new Action(actionDto, command);

        assertEquals("an_action", actionDto.getName());
        assertEquals("an_action", minimalAction.getName());

        assertEquals("Yep, it's an action all right", actionDto.getDescription());
        assertEquals("Yep, it's an action all right", minimalAction.getDescription());

        assertEquals((Long)command.getId(), actionDto.getCommandId());
        assertEquals(command, minimalAction.getCommand());

        assertEquals(root, actionDto.getRoot());
        assertEquals(root, minimalAction.getRoot());

        assertThat(actionDto.getInputs(), nullValue());
        assertThat(minimalAction.getInputs(), notNullValue());
        assertThat(defaultInputs, everyItem(isIn(minimalAction.getInputs())));

        assertThat(actionDto.getResourcesStaged(), nullValue());
        assertThat(minimalAction.getResourcesStaged(), hasSize(1));
        assertEquals(defaultResourceStaged, minimalAction.getResourcesStaged().get(0));
        assertThat(actionDto.getResourcesCreated(), nullValue());
        assertThat(minimalAction.getResourcesCreated(), hasSize(1));
        assertEquals(defaultResourceCreated, minimalAction.getResourcesCreated().get(0));
    }

    @Test
    public void testPersistAction() throws Exception {
        final DockerImage image = mapper.readValue(DOCKER_IMAGE_JSON, DockerImage.class);
        dockerImageService.create(image);

        final String commandJson = String.format(COMMAND_JSON_TEMPLATE, image.getId());
        final Command command = mapper.readValue(commandJson, Command.class);
        commandService.create(command);

        final String actionJson = String.format(ACTION_JSON_TEMPLATE, command.getId());
        final ActionDto actionDto = mapper.readValue(actionJson, ActionDto.class);
        final Action action = actionService.createFromDto(actionDto);
        actionService.flush();
        actionService.refresh(action);

        final Action retrievedAction = actionService.retrieve(action.getId());

        assertEquals(action, retrievedAction);
        assertNotNull(action.getCommand());
        assertEquals(command, action.getCommand());
    }
}
