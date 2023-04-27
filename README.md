# Uploader

![Gradle Build](https://github.com/cyface-de/uploader/actions/workflows/gradle_build.yml/badge.svg)(https://github.com/cyface-de/uploader/actions/workflows/gradle_build.yml)
![Gradle Publish](https://github.com/cyface-de/uploader/actions/workflows/gradle_publish.yml/badge.svg)(https://github.com/cyface-de/uploader/actions/workflows/gradle_publish.yml)

The Cyface Uploader is used to upload the [Cyface Binary](https://github.com/cyface-de/serialization) to the [Cyface
Collector](https://github.com/cyface-de/data-collector).

If you require this software under a closed source license for you own
projects, please [contact us](https://www.cyface.de/#kontakt).

Changes between versions are found in the [Release
Section](https://github.com/cyface-de/uploader/releases).

The project uses [Gradle](https://gradle.org/) as the build system.

# Overview

_Libraries_
- [Uploader](#uploader)

_General information_
- [Release a new Version](#release-a-new-version)
- [Publishing Artifacts to GitHub Packages
  manually](#publishing-artifacts-to-github-packages-manually)
- [Licensing](#licensing)

-----------------------------------------------------------------------------

## Uploader

The uploader library which is used in the [Cyface Android
SDK](https://github.com/cyface-de/android-backend) and uploads data to a
[Cyface Collector](https://github.com/cyface-de/data-collector).


## Release a new Version

See [Cyface Collector
Readme](https://github.com/cyface-de/data-collector#release-a-new-version)

- `version` in root `build.gradle` is automatically set by the CI
- Just tag the release and push the tag to Github
- The Github package is automatically published when a new version is
  tagged and pushed by our [Github
  Actions](https://github.com/cyface-de/uploader/actions) to the [Github
  Registry](https://github.com/cyface-de/uploader/packages)
- The tag is automatically marked as a *new Release* on
  [Github](https://github.com/cyface-de/uploader/releases)


## Publishing artifacts to GitHub Packages manually

The artifacts produced by this project are distributed via
[GitHubPackages](https://github.com/features/packages). Before you can
publish artifacts you need to rename `gradle.properties.template` to
`gradle.properties` and enter your GitHub credentials. How to obtain
these credentials is described
[here](https://help.github.com/en/github/managing-packages-with-github-packages/about-github-packages#about-tokens).

To publish a new version of an artifact you need to:

1.  Increase the version number of the subproject within the
    `build.gradle` file

2.  Call `./gradlew publish`

This will upload a new artifact to GitHub packages with the new version.
GitHub Packages will not accept to overwrite an existing version or to
upload a lower version. This project uses [semantic
versioning](https://semver.org/).


## Licensing

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
