# Changelog

## 1.4.1

Not yet released.

### Features

* [CS-461][] and [CS-462][] Change handling of image entrypoint. (Reverts changes introduced in 1.4.0 by [CS-433][].) For discussion of this issue, see this [xnat_discussion board post][entrypoint-post].
    The APIs we had been using are these for containers...
    ```
    Image.Cmd = ["/bin/sh", "-c", "COMMAND"]
    Image.Entrypoint = [""]
    ```
    And these for swarm services...
    ```
    ContainerSpec.Command = ["/bin/sh", "-c", "COMMAND"]
    ContainerSpec.Args = null
    ```
    This caused the image entrypoint to be overriden in all cases, with no recourse for the command author. With this pair of changes, we change the way we use the APIs to launch containers in all cases, but also provide an optional property on the Command (`"override-entrypoint"`) for whether to override the entrypoint or not (defaulting to `false`, i.e. do not override). So now, whether overriding the entrypoint or not, we pass the resolved command-line string to this API for containers...
    ```
    Image.Cmd = COMMAND (split into tokens like a shell would)
    ```
    Depending on whether the entrypoint is overridden or not, we set this value for containers...
    ```
    Image.Entrypoint = [""] (to override the entrypoint, or)
    Image.Entrypoint = null (to leave the entrypoint it as is)
    ```
    For swarm services, we have to use different APIs depending on whether we override the entrypoint or not. When not overriding, we use...
    ```
    ContainerSpec.Command = null
    ContainerSpec.Args = COMMAND (split into tokens like a shell would)
    ```
    When overriding, we use
    ```
    ContainerSpec.Command = ["/bin/sh", "-c", COMMAND] (command not split into tokens, because the /bin/sh will do that)
    ContainerSpec.Args = null
    ```

* Add option to reserve memory and/or limit memory and CPU usage of containers via command entries "reserve-memory", "limit-memory", "limit-cpu". Update command documentation accordingly. (https://github.com/NrgXnat/container-service/pull/6)

### Bugfixes

*

[CS-461]: https://issues.xnat.org/browse/CS-461
[entrypoint-post]: https://groups.google.com/forum/#!msg/xnat_discussion/NBVjAS8gXhU/Zu7xJngCAgAJ

## 1.4.0

[Released 2018-01-05](https://github.com/NrgXnat/container-service/releases/tag/1.4.0).

### Features

* [CS-421][] Add Setup Commands. These are special commands that can be used to pre-process files from XNAT before they are mounted into a container launched from a "main" command.
* [CS-355][] When deleting a docker image through CS API, also delete all commands and wrappers associated with the image.
* Docs: Add new script that bulk uploads all changed source docs
* Docs: Add support for "NO UPLOAD" comment in source docs that should be skipped and not uploaded to wiki
* [CS-340][] Check in generated swagger.json from REST API dump, and scripts to upload it to wiki
* Add lots of new documentation
    * [CS-434][] Command resolution
    * [CS-434][] Container launching (internals)
    * Container launching (as a user)
    * Enabling commands on a project
    * Troubleshooting

### Bugfixes

* Docs: Fix mishandling of anchor tags/links on wiki pages. Confluence uses a macro for these, not raw HTML anchors.
* [CS-420][] Fix handling of non-standard default values for boolean inputs in bulk launches
* [CS-432][] Do not update container/workflow status if new events come in from docker and container is already in a "terminal" state (Completed, Failed, Killed, or related states)
* [CS-433][] Remove image entrypoints when launching containers.
* [CS-435][] Command wrappers with no descriptions were not displaying properly in the UI
* [CS-442][], [CS-443][], [CS-449][] Multiple failures when running XNAT in a non-root context: pulling images, project settings, bulk launching
* [CS-448][] Fix height of 'Edit Command' dialog so full code editor is displayed
* [CS-450][] Restrict usage of command automation to commands that match the context of selected events
* [CS-454][] Container working directory was not being saved at launch

[CS-340]: https://issues.xnat.org/browse/CS-340
[CS-355]: https://issues.xnat.org/browse/CS-355
[CS-420]: https://issues.xnat.org/browse/CS-420
[CS-421]: https://issues.xnat.org/browse/CS-421
[CS-430]: https://issues.xnat.org/browse/CS-430
[CS-432]: https://issues.xnat.org/browse/CS-432
[CS-433]: https://issues.xnat.org/browse/CS-433
[CS-434]: https://issues.xnat.org/browse/CS-434
[CS-435]: https://issues.xnat.org/browse/CS-435
[CS-442]: https://issues.xnat.org/browse/CS-442
[CS-443]: https://issues.xnat.org/browse/CS-443
[CS-448]: https://issues.xnat.org/browse/CS-448
[CS-449]: https://issues.xnat.org/browse/CS-449
[CS-450]: https://issues.xnat.org/browse/CS-450
[CS-454]: https://issues.xnat.org/browse/CS-454

## 1.3.2

[Released 2017-11-08](https://github.com/NrgXnat/container-service/releases/tag/1.3.2).

### Features

* [CS-282][] Deprecated the "Project Opt-in" setting that wanted to set a default behavior for projects when a new command was added to a site. The UI existed and appeared to do things but was not tied to a functional API.

### Bugfixes

* [CS-335][] Fix permissions errors with command project configuration
* [CS-348][] Ensure that project defaults would be used for container launches where present, instead of defaulting to site-wide defaults
* [CS-389][] Set ":latest" as the default tag value when importing an image
* [CS-392][] Improve behavior of dialog when removing project-specific defaults for a command
* [CS-399][] Refresh image list when deleting a command config
* [CS-410][] Fix command update so all fields in all child objects can update too
* [CS-402][] Commands launched automatically on session archive now create workflows
* [CS-402][] Remove en-dash from scan label. This allows automated launches to succeed.
* [CS-414][] Fix bug that caused command automation button to disappear when panel refreshed
* [CS-415][] Do not read old container events after updating docker server settings
* [CS-416][] Fix a lot of extraneous error messages when launching containers on scans of the form "Cannot construct a (whatever) URI. Parent URI is null."
* [CS-418][] Fix a bug with the handling of boolean checkboxes in the command launch UI"

[CS-282]: https://issues.xnat.org/browse/CS-282
[CS-335]: https://issues.xnat.org/browse/CS-335
[CS-348]: https://issues.xnat.org/browse/CS-348
[CS-389]: https://issues.xnat.org/browse/CS-392
[CS-392]: https://issues.xnat.org/browse/CS-392
[CS-399]: https://issues.xnat.org/browse/CS-392
[CS-402]: https://issues.xnat.org/browse/CS-402
[CS-410]: https://issues.xnat.org/browse/CS-410
[CS-414]: https://issues.xnat.org/browse/CS-414
[CS-415]: https://issues.xnat.org/browse/CS-415
[CS-416]: https://issues.xnat.org/browse/CS-416
[CS-418]: https://issues.xnat.org/browse/CS-418

## 1.3.1

[Released 2017-10-11](https://github.com/NrgXnat/container-service/releases/tag/1.3.1).

### Features

* [CS-376][] Enable bulk launching of containers from data tables on the project report page. Any filters that the user sets on these tables will be reflected in the list of items that get prepared for launch. The user can then confirm, select or deselect any of those items before launching containers. This feature also works for any project-specific stored search.

[CS-376]: https://issues.xnat.org/browse/CS-376

### Bugfixes

* [CS-395][] Catch a rare error that causes the search ID for a project data table to expire within an active session. This interrupts the user's ability to launch a batch of containers from this table.
* [CS-396][] Update text of buttons and dialogs related to bulk launches.

[CS-395]: https://issues.xnat.org/browse/CS-395
[CS-396]: https://issues.xnat.org/browse/CS-396

## 1.3.0

[Released 2017-09-29](https://github.com/NrgXnat/container-service/releases/tag/1.3.0).

### Features

* [CS-1][] Docker swarm integration. You can configure the container service to launch services on your docker swarm by pointing it to your swarm master and setting Swarm Mode to true. This is a global setting; when swarm mode is turned on all jobs will be run as services sent to the swarm master, when it is off all jobs will be run as containers on the docker server. In addition, the docker server `ping` function in swarm mode will check not only if the specified server is reachable, but also that it is configured as a swarm master (i.e. it has to respond correctly to `docker node ls`).
* [CS-377][] Allow editing of command definitions through the Admin UI. In order to make this work, a few fields have to be edited out of the command definition to prevent an ID conflict.
* [CS-368][] Add date filtering and sorting to container history table.
* [CS-381][] Improve layout of images panel in Admin UI
* [CS-383][] Allow command wrappers to use the context `"site"` to run at a site-wide context. Unlike wrappers with other datatype-specific contexts, these will not receive any object as an external input at runtime.

[CS-1]: https://issues.xnat.org/browse/CS-1
[CS-368]: https://issues.xnat.org/browse/CS-368
[CS-377]: https://issues.xnat.org/browse/CS-377
[CS-381]: https://issues.xnat.org/browse/CS-381
[CS-383]: https://issues.xnat.org/browse/CS-383

### Bugfixes

* [CS-382][] Fix command update API

[CS-382]: https://issues.xnat.org/browse/CS-382

## 1.2.1

[Released 2017-09-06](https://github.com/NrgXnat/container-service/releases/tag/1.2.1).

### Features

* [CS-343][] Whenever a container is launched, create a workflow. Keep the status updated as the container's status changes. This allows us to piggyback on the existing workflow UI (history table, alert banner, etc.) to display container status.
* [CS-359][] Docker server is now stored in a hibernate table rather than as a prefs bean. This should ease a possible future transition to multiple docker servers.
* [CS-346][] Project owners that have set new default run-time settings for command configurations can reset those settings to the site-wide defaults.
* [CS-374][] Add lots of properties to the xnat model objects, which are now available to use in commands:
    * Assessor
        * `project-id`
        * `session-id`
    * Session
        * `subject-id`
    * Scan
        * `project-id`
        * `session-id`
        * `frames`
        * `note`
        * `modality`
        * `quality`
        * `scanner`
        * `scanner-manufacturer`
        * `scanner-model`
        * `scanner-software-version`
        * `series-description`
        * `start-time`
        * `uid`

[CS-343]: https://issues.xnat.org/browse/CS-343
[CS-346]: https://issues.xnat.org/browse/CS-346
[CS-359]: https://issues.xnat.org/browse/CS-359
[CS-374]: https://issues.xnat.org/browse/CS-374

### Bugfixes

* [CS-349][] Assessor model objects have URLs that start with their parent session's `/experiments/{sessionId}` URL. This allows containers to be run on assessors, as long as the assessor has a defined resource directory that can be mounted.
* [CS-352][] `GET /docker/hubs` and `GET /docker/hubs/{id}` return correct `ping`.
* [CS-373][] Docker events will only be recorded in container history once.
* [CS-295][], [CS-353][] Only enable the command automation panel if commands are available to use in the container service, and only allow automations to use enabled commands.
* [CS-367][] Fix the display issues that caused long command labels to get cut off in the Actions Box "Run Containers" menu.
* [CS-351][] Don't automatically treat new image hosts as the default image host in the Admin control panel.

[CS-295]: https://issues.xnat.org/browse/CS-295
[CS-349]: https://issues.xnat.org/browse/CS-349
[CS-351]: https://issues.xnat.org/browse/CS-351
[CS-352]: https://issues.xnat.org/browse/CS-352
[CS-353]: https://issues.xnat.org/browse/CS-353
[CS-367]: https://issues.xnat.org/browse/CS-367
[CS-373]: https://issues.xnat.org/browse/CS-373

## 1.2

[Released 2017-08-18](https://github.com/NrgXnat/container-service/releases/tag/1.2).

### Features

* [CS-356][] This version, and forseeable future versions, will continue to support XNAT 1.7.3.
* Ping server on `GET /docker/server` and create
* [CS-111][], [CS-285][] Ping hub on /xapi/hubs operations and reflect that ping status in Admin UI
* [CS-62][] Special-case an error when someone wants to POST to /images/save but instead GETs /images/save.
* [CS-215][] POST /xapi/docker/pull will now return a 404 rather than a 500 when it cannot find the image you are attempting to pull
* [CS-318][] Containers returned from `GET /xapi/containers` and `GET /xapi/containers/{id}` now include top-level `status` property. This is mostly equal to the status in the most recent history item on the container, except for mapping the docker event statuses to more user-friendly statuses.
    * `created` -> `Created`
    * `started` -> `Running`
    * `die` -> `Done`
    * `kill` -> `Killed`
    * `oom` -> `Killed (Out of memory)`
* [CS-227][] Container inputs are no longer recorded with type `wrapper`. Now all wrapper inputs will be recorded as either `wrapper-external` or `wrapper-derived`. Containers launched and recorded before this version will still have inputs of type `wrapper`; we can't migrate them because information has been lost.
* [CS-145][], [CS-146][] Add convenience functions to the UI to enable / disable all commands in a site or in a project
* [CS-256][], [CS-307][] Force users to confirm delete request on an image that has had containers run in that XNAT
* [CS-205][], [CS-281][] Allow admins to view container logs directly in the container history table

[CS-62]: https://issues.xnat.org/browse/CS-62
[CS-111]: https://issues.xnat.org/browse/CS-111
[CS-145]: https://issues.xnat.org/browse/CS-145
[CS-146]: https://issues.xnat.org/browse/CS-146
[CS-205]: https://issues.xnat.org/browse/CS-205
[CS-215]: https://issues.xnat.org/browse/CS-215
[CS-227]: https://issues.xnat.org/browse/CS-227
[CS-256]: https://issues.xnat.org/browse/CS-256
[CS-281]: https://issues.xnat.org/browse/CS-281
[CS-285]: https://issues.xnat.org/browse/CS-285
[CS-307]: https://issues.xnat.org/browse/CS-307
[CS-318]: https://issues.xnat.org/browse/CS-318
[CS-356]: https://issues.xnat.org/browse/CS-356

### Bugfixes

* [CS-263][] When finalizing a container, check if user can edit parent before creating new resource. (Sorry, I thought I had already done this.)
* [CS-347][] Prevent project settings from overwritingn site-wide preferences for command configurations
* [XXX-60][], [XXX-61][] Prevent users from launching containers with input values containing strings that could be used for arbitrary code execution:

        ;
        `
        &&
        ||
        (
* [CS-349][] Better handle `/experiment/{assessor id}` URIs as inputs for assessor external inputs. (They will not be able to mount files, though. See [CS-354])
* [CS-350][] More reliably find assessor directories to mount

* [CS-337][] Fail more gracefully when user tries to use a docker host url with no scheme. An error message is logged that is not super helpful right now, but will become a little bit more helpful in a future version once [spotify/docker-client#870][] is merged and released.
* [CS-351][] Fix a bug with the "Default Image Host" toggle
* [CS-279][] Explicitly restrict access to container launch UI to project users with edit permissions (i.e. owners and members)
* [CS-297][] Fix the ability to delete a command configuration. Improve appearance of "Delete" buttons throughout the admin UI
* [CS-293][] Remove non-functional "Delete" buttons from instances in the UI where it was not supported
* [CS-341][] `GET /{some identifiers}/launch` returns `user-settable` boolean property on inputs
* [CS-339][], [CS-340][] Fix faulty display and handling of non-user-settable inputs in container launch UI
* [CS-274][] Improve functioning of Project opt-in settings.
* [CS-212][] Improve display and readability of error messages throughout
* [CS-317][], [CS-273][] Taxonomy cleanup in UI for consistency

[CS-212]: https://issues.xnat.org/browse/CS-212
[CS-263]: https://issues.xnat.org/browse/CS-263
[CS-273]: https://issues.xnat.org/browse/CS-273
[CS-274]: https://issues.xnat.org/browse/CS-274
[CS-279]: https://issues.xnat.org/browse/CS-279
[CS-293]: https://issues.xnat.org/browse/CS-293
[CS-297]: https://issues.xnat.org/browse/CS-297
[CS-317]: https://issues.xnat.org/browse/CS-317
[CS-337]: https://issues.xnat.org/browse/CS-337
[CS-339]: https://issues.xnat.org/browse/CS-339
[CS-340]: https://issues.xnat.org/browse/CS-340
[CS-341]: https://issues.xnat.org/browse/CS-341
[CS-347]: https://issues.xnat.org/browse/CS-347
[CS-349]: https://issues.xnat.org/browse/CS-349
[CS-350]: https://issues.xnat.org/browse/CS-350
[CS-351]: https://issues.xnat.org/browse/CS-351
[CS-354]: https://issues.xnat.org/browse/CS-354
[XXX-60]: https://issues.xnat.org/browse/XXX-60
[XXX-61]: https://issues.xnat.org/browse/XXX-61
[spotify/docker-client#870]: https://github.com/spotify/docker-client/pull/870

## 1.1

[Released 2017-08-04](https://github.com/NrgXnat/container-service/releases/tag/1.1).

### Features

* Integrated UI container launcher controls with the [Selectable Table plugin](https://bitbucket.org/xnatdev/selectable-table-plugin) to allow users to run containers on individual scans or multiple selected scans in bulk.
* Added master controls in the Admin UI and Project Settings UI to enable and disable all available commands for individual projects and at the site-wide level.
* [CS-286][] Remove unused enable-all/disable-all APIs
* [CS-288][] Project enabled API now returns an object with project enabled boolean, project name, and command enabled boolean.
* Change launch report format. Now instead of showing either wrapper ID or command ID + wrapper name (depending on which API you used to launch the container), the launch report always shows wrapper ID and command ID. The IDs are now formatted as integers, not strings.

[CS-286]: https://issues.xnat.org/browse/CS-286
[CS-288]: https://issues.xnat.org/browse/CS-288

### Bugfixes

* [CS-282][] (Not actually fixed yet.) The existing (unused) `/container-service/settings` API has been removed. This clears the way for the API to be refactored in the next version.
* [CS-289][] Mounting a session no longer mounts the project
* Mark command and wrapper descriptions in LaunchUi as Nullable. This prevents a potential bug that I haven't yet seen.

[CS-282]: https://issues.xnat.org/browse/CS-282
[CS-289]: https://issues.xnat.org/browse/CS-289

## 1.0

[Released 2017-07-28](https://github.com/NrgXnat/container-service/releases/tag/v1.0).

### Features

* Added Changelog
* Check if command exists before saving, rather than catching exception when it fails to save.
* [CS-205][] Add `/containers/{id}/logs` and `/containers/{id}/logs/{stdout|stderr}` APIs to fetch container logs.
* Rename wrapper input properties to remove "xnat":
    * `derived-from-xnat-input` -> `derived-from-wrapper-input`
    * `as-a-child-of-xnat-input` -> `as-a-child-of-wrapper-input`
* "Create docker server" API returns 201 on success, not 202.
* Add "directory" property to Session and Assessor model objects

[CS-205]: https://issues.xnat.org/browse/CS-205

### Bugfixes

* Add some additional null checks to services
* [CS-177][] Fix for untagged images showing as ':' in UI
* [CS-276][] Correctly track command and wrapper identifiers in LaunchReport
* Could not read containers from db. Broken by 3385972b.
* Wrapper database tables were broken after 7b9a9a28.

[CS-177]: https://issues.xnat.org/browse/CS-177
[CS-276]: https://issues.xnat.org/browse/CS-276

## 1.0-alpha.3-hotfix
[Released 2017-07-25](https://github.com/NrgXnat/container-service/releases/tag/v1.0-alpha.3-hotfix).

### Features

* [CS-242][] Add REST API to support bulk container launch UI
* Add resiliency to launcher if no default value is provided for a child input

[CS-242]: https://issues.xnat.org/browse/CS-242

### Bugfixes
* [CS-257][] Hide “Create Automation” button from project owners if they do not have admin privileges. Depends on [XNAT-5044](https://issues.xnat.org/browse/XNAT-5044) change.
* Fix: container -> container entity should use existing entity in db if it exists
* Fix: Initialize container entity when retrieving by container id

[CS-257]: https://issues.xnat.org/browse/CS-257

## 1.0-alpha.3

Let's consider this the first version for purposes of this changelog. Because we haven't kept track of what has changed from the beginning of the repo until now. You can always go dig through the commits if you are really curious.
