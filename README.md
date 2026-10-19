# Uploader

[![Gradle Build](https://github.com/cyface-de/uploader/actions/workflows/gradle_build.yml/badge.svg)](https://github.com/cyface-de/uploader/actions/workflows/gradle_build.yml)
[![Gradle Publish](https://github.com/cyface-de/uploader/actions/workflows/gradle_publish.yml/badge.svg)](https://github.com/cyface-de/uploader/actions/workflows/gradle_publish.yml)

This project contains the Cyface Uploader which is used by the [Cyface Android SDK](https://github.com/cyface-de/android-backend) to upload [Cyface Binaries](https://github.com/cyface-de/serialization) to the [Cyface
Collector](https://github.com/cyface-de/data-collector).

If you require this software under a closed source license for you own
projects, please [contact us](https://www.cyface.de/#kontakt).

Changes between versions are found in the [Release
Section](https://github.com/cyface-de/uploader/releases).

# Overview

- [Integration Guide](#integration-guide)
- [API Usage Guide](#api-usage-guide)
- [Developer Guide](#developer-guide)
  - [Release a new Version](#release-a-new-version)
  - [Publishing Artifacts to GitHub Packages
  manually](#publishing-artifacts-to-github-packages-manually)
- [License](#license)

-----------------------------------------------------------------------------

## Integration Guide

This library is published to the Github Package Registry.

To use it as a dependency in your app you need to:

1.  Make sure you are authenticated to the repository:

  - You need a Github account with read-access to this Github
    repository

  - Create a [personal access token on
    Github](https://github.com/settings/tokens) with "read:packages"
    permissions

  - Create or adjust a `gradle.properties` file in the project root
    containing:

    <!-- -->

         gpr.user=YOUR_USERNAME
         gpr.key=YOUR_ACCESS_TOKEN

  - Add the custom repository to your `build.gradle`:

    <!-- -->

         repositories {
             // Other maven repositories, e.g.:
             mavenCentral()
             // Repository for this library
             maven {
                 url = uri("https://maven.pkg.github.com/cyface-de/uploader")
                 credentials {
                     username = project.findProperty("gpr.user")
                     password = project.findProperty("gpr.key")
                 }
             }
         }

2.  Add this package as a dependency to your `build.gradle`:

         dependencies {
             implementation "de.cyface:uploader:$cyfaceUploaderVersion"
         }

3.  Set the `cyfaceUploaderVersion` gradle variable to the
    [latest
    version](https://github.com/cyface-de/uploader/releases).


## API Usage Guide

- [Collector Compatibility](#collector-compatibility)

- [Authentication](#authentication)

- [Uploader](#uploader)

### Collector Compatibility

This library is compatible with the [Cyface Collector Version 6](https://github.com/cyface-de/data-collector/releases/tag/6.0.0).

### Authentication

The uploader needs a JWT access token of the OAuth provider of the Collector (Keycloak). The library does not request the token; get it before each upload, for example with the OAuth client of your app.

### Uploader

```kotlin
val uploader = DefaultUploader("https://example.cyface.de/api/v4")
val progressListener = object : UploadProgressListener {
    override fun updatedProgress(percent: Float) {}
}

val measurement = Measurement(
    MeasurementIdentifier(UUID.fromString("1e1abeb0-469b-4d5d-b4c4-9bd3ebdcfd07"), 1L),
    DeviceMetaData("Android 13", "Pixel 6"),
    ApplicationMetaData("3.2.0", ApplicationMetaData.CURRENT_TRANSFER_FILE_FORMAT_VERSION),
    MeasurementMetaData(
        length = 985.7357616144405,
        locationCount = 232,
        startLocation = GeoLocation(1637744753012L, 51.1, 13.1),
        endLocation = GeoLocation(1637744993000L, 51.2, 13.2),
        modality = "BICYCLE",
    ),
    AttachmentMetaData(logCount = 0, imageCount = 0, videoCount = 0, filesSize = 0L),
)

// The Cyface Binary of the measurement
val binary = File("/path/to/measurement.cyf")

// Upload the measurement. Attachments are uploaded with `uploadAttachment()`.
val result = uploader.uploadMeasurement(token, measurement, binary, progressListener)
```

The uploader sends the measurement to `<apiEndpoint>/measurements` and an attachment to `<apiEndpoint>/measurements/<deviceId>/<measurementId>/attachments`.

The result is

- `Result.UPLOAD_SUCCESSFUL` when the upload was successful or the data already exists on the server
- `Result.UPLOAD_SKIPPED` when the server is not interested in the data (e.g. data without locations)

In case of an error, an `UploadFailed` exception will be thrown.

*The cause for the `UploadFailed` may be one of the following (the list may not be complete or stable):*

- `SynchronisationException` - If an IOException occurred during synchronization. This usually means that you should try again later.
- `BadRequestException` - When server returns `HttpURLConnection#HTTP_BAD_REQUEST` (`400`)
- `UnauthorizedException` - When the server returns `HttpURLConnection#HTTP_UNAUTHORIZED` (`401`)
- `ForbiddenException` - When the server returns `HttpURLConnection#HTTP_FORBIDDEN` (`403`)
- `ConflictException` - When the server returns `HttpURLConnection#HTTP_CONFLICT` (`409`)
- `EntityNotParsableException` - When the server returns `DefaultUploader.HTTP_ENTITY_NOT_PROCESSABLE` (`422`)
- `InternalServerErrorException` - When the server returns `HttpURLConnection#HTTP_INTERNAL_ERROR` (`500`)
- `NetworkUnavailableException` - When the network used for transmission becomes unavailable. This usually means that you should try again later.
- `SynchronizationInterruptedException` - When the transmission stream ended too early, likely because the sync thread was interrupted (sync canceled). This usually means that you should try again later.
- `TooManyRequestsException` - When the server returns `DefaultUploader.HTTP_TOO_MANY_REQUESTS` (`429`)
- `HostUnresolvable` - e.g. when the phone is connected to a network which is not connected to the internet (NOTE: Currently, a more generic `SynchronisationException` is thrown instead.)
- `ServerUnavailableException` - When no connection could be established with the server (`java.net.SocketTimeoutException`)
- `MeasurementTooLarge` - When the transfer file is too large to be uploaded. (NOTE: The max size is currently static and set to 100 MB which should be about 44 hours of 100 Hz measurement)
- `UnexpectedResponseCode` - When the server returns an unexpected response code (none of the mentioned above)
- `AccountNotActivated` - When the user account is not activated (`428`)
- `UploadSessionExpired` - When the upload session on the server expired. Start the upload again.

Before the upload, the model classes throw `InvalidMetaData`, `TooFewLocations`, `DeprecatedFormatVersion` or `UnknownFormatVersion` when the metadata is not valid.


## Developer Guide

This section is only relevant for developers of this library.

The library uses [Gradle](https://gradle.org/) as the build system (Gradle 8.13, Kotlin 2.1.20, JVM target Java 11).

Copy `gradle.properties.template` to `gradle.properties` and add your GitHub credentials.

```bash
./gradlew clean build                                            # build and run the tests
./gradlew test --tests "de.cyface.uploader.DefaultUploaderTest"  # one test class
./gradlew detekt                                                 # static analysis
```

Architecture (package `de.cyface.uploader`):

- `Uploader` -- the interface: `uploadMeasurement()`, `uploadAttachment()`, the endpoint URLs and `onUploadFinished()`.
- `DefaultUploader` -- the implementation. It uses the resumable upload of the Google API Client (`MediaHttpUploader`): the metadata goes as JSON in the pre-request, the file follows in one chunk.
- `model/` -- `Measurement` and `Attachment` (the `Uploadable` types) and the metadata value objects in `model/metadata/`.
- `exception/` -- `UploadFailed` wraps all upload errors; the other exceptions map to HTTP status codes and network errors.

Constraints:

- The file is uploaded in one chunk of 100 MB at most, because the server cannot join more than one chunk.
- Detekt allows no issues (`maxIssues: 0`), the line length is 120 characters at most (`config/detekt.yml`).
- Tests use JUnit 5, Hamkrest and Mockito Kotlin.

### GitHub Workflows

- `gradle_build.yml` builds and tests each push and pull request to `main` and `release-*`. Pull requests from forks are skipped.
- `gradle_publish.yml` runs on version tags: it publishes the library to GitHub Packages and creates the GitHub release.

Rules of the workflows (hardening of CY-6748): do not use `${{ github.ref_name }}` or other event values directly in a `run:` script, pass them through `env:`; pass secrets through `env:` of the step that needs them; use `runs-on: ubuntu-26.04`, not `ubuntu-latest`.

The organization-wide cleanup in `cyface-de/production` (`.github/workflows/registry_cleanup.yml`) deletes old package versions. A version that a build file of the organization names stays.

### Release a new Version

- The CI sets the `version` in the root `build.gradle.kts` from the tag. Do not change it manually.
- Create an annotated tag with a semantic version (for example `1.6.3`) on `main` and push it. The body of the tag message becomes the release notes.
- The workflow publishes the library to the [Github
  Registry](https://github.com/cyface-de/uploader/packages) and creates the [GitHub release](https://github.com/cyface-de/uploader/releases).
- A tag with a suffix (for example `1.6.3-beta1`) becomes a pre-release.


### Publishing artifacts to GitHub Packages manually

The artifacts produced by this project are distributed via
[GitHubPackages](https://github.com/features/packages). Before you can
publish artifacts you need to rename `gradle.properties.template` to
`gradle.properties` and enter your GitHub credentials. Use a personal access token (classic) with the scope `write:packages`, as described
[here](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-gradle-registry#authenticating-to-github-packages).

To publish a new version of an artifact you need to:

1.  Increase the version number of the subproject within the
    `build.gradle.kts` file

2.  Call `./gradlew publish`

This will upload a new artifact to GitHub packages with the new version.
GitHub Packages will not accept to overwrite an existing version or to
upload a lower version. This project uses [semantic
versioning](https://semver.org/).


## License

Copyright 2023-2026 Cyface GmbH

This file is part of the Cyface Uploader.

The Cyface Uploader is free software: you can redistribute it and/or
modify it under the terms of the GNU General Public License as published
by the Free Software Foundation, either version 3 of the License, or (at
your option) any later version.

The Cyface Uploader is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
Public License for more details.

You should have received a copy of the GNU General Public License along
with the Cyface Uploader. If not, see <http://www.gnu.org/licenses/>.
