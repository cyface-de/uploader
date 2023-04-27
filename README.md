# Uploader

![Gradle Build](https://github.com/cyface-de/uploader/actions/workflows/gradle_build.yml/badge.svg)(https://github.com/cyface-de/uploader/actions/workflows/gradle_build.yml)
![Gradle Publish](https://github.com/cyface-de/uploader/actions/workflows/gradle_publish.yml/badge.svg)(https://github.com/cyface-de/uploader/actions/workflows/gradle_publish.yml)

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

- [Authenticator](#authenticator)

- [Uploader](#uploader)

### Collector Compatibility

This library is compatible with the [Cyface Collector Version 6](https://github.com/cyface-de/data-collector/releases/tag/6.0.0).

### Authenticator

The Authenticator allows to request an auth token from the Cyface Auth API (currently part of the Cyface Collector).

You need such an auth token to upload data to the Collector API.

In case you use a custom authentication just skip this section and continue with the [Uploader](#uploader).

```kotlin
val apiEndpoint = "https://example.cyface.de/api/v3"
val authenticator = DefaultAuthenticator(apiEndpoint)

// Request a new token just before each upload attempt
val token = authenticator.authenticate(username, password)
```

### Uploader

```kotlin
val apiEndpoint = "https://example.cyface.de/api/v3"
val uploader = DefaultUploader(apiEndpoint)
val processListener = object : UploadProgressListener {
    override fun updatedProgress(percent: Float) {}
}

// Example token and metadata
val token = "eyXyz123***"
val deviceId = "1e1abeb0-469b-4d5d-b4c4-9bd3ebdcfd07"
val measurementId = "1"
val osVersion = "Android 13"
val deviceType = "Pixel 6"
val appVersion = "3.2.0"
val length = 985.7357616144405
val locationCount = 232
val startLocation = GeoLocation(1637744753012L, 51.1, 13.1)
val endLocation = GeoLocation(1637744993000, 51.2, 13.2)
val modality = "BICYCLE"
val formatVersion = RequestMetaData.CURRENT_TRANSFER_FILE_FORMAT_VERSION
val metaData = RequestMetaData(
    deviceId, measurementId,
    osVersion, deviceType, appVersion, length,
    locationCount, startLocation, endLocation,
    modality, formatVersion
)

// Replace with a `*.ccyf` [Cyface Binary](https://github.com/cyface-de/serialization) or another binary when using a custom Data Collector
val binary: File = null

// Upload requests
val result = uploader.upload(token, metaData, binary, processListener)
```

The result is

- `Result.UPLOAD_SUCCESSFUL` when the upload was successful
- `Result.UPLOAD_SKIPPED` when the server is not interested in the data (e.g. data without locations)

In case of an error, an exception will be thrown:

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


## Developer Guide

This section is only relevant for developers of this library.

The library uses [Gradle](https://gradle.org/) as the build system

### Release a new Version

See [Cyface Collector
Readme](https://github.com/cyface-de/data-collector#release-a-new-version)

- `version` in root `build.gradle.kts` is automatically set by the CI
- Just tag the release and push the tag to Github
- The Github package is automatically published when a new version is
  tagged and pushed by our [Github
  Actions](https://github.com/cyface-de/uploader/actions) to the [Github
  Registry](https://github.com/cyface-de/uploader/packages)
- The tag is automatically marked as a *new Release* on
  [Github](https://github.com/cyface-de/uploader/releases)


### Publishing artifacts to GitHub Packages manually

The artifacts produced by this project are distributed via
[GitHubPackages](https://github.com/features/packages). Before you can
publish artifacts you need to rename `gradle.properties.template` to
`gradle.properties` and enter your GitHub credentials. How to obtain
these credentials is described
[here](https://help.github.com/en/github/managing-packages-with-github-packages/about-github-packages#about-tokens).

To publish a new version of an artifact you need to:

1.  Increase the version number of the subproject within the
    `build.gradle.kts` file

2.  Call `./gradlew publish`

This will upload a new artifact to GitHub packages with the new version.
GitHub Packages will not accept to overwrite an existing version or to
upload a lower version. This project uses [semantic
versioning](https://semver.org/).


## License

Copyright 2023 Cyface GmbH

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
