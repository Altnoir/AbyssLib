# Third-party notices

AbyssLib contains third-party source code. The root `LICENSE` covers AbyssLib's
original MIT-licensed code; it does not replace the licenses of the components
listed below. Each component's original authors retain their copyrights.
Licenses apply to their respective files, not as a choice between MIT and MPL.

This notice accompanies every module for convenience. Registrate is included in
Reginth and the aggregate AbyssLib JAR; Athena-derived code is included in ReLink
and the aggregate JAR. Atlas and the aggregate's own facade use AbyssLib's MIT
license.

## Registrate / Reginth

- Upstream: https://github.com/tterrag1098/Registrate
- Authors: tterrag1098 and the Registrate contributors.
- Imported version: `MC26.1-1.5.7`, imported from the upstream sources JAR.
- License: Mozilla Public License 2.0 (`MPL-2.0`).
- Complete license: `LICENSES/Registrate-MPL-2.0.txt` in this repository;
  `META-INF/licenses/Registrate-MPL-2.0.txt` in distributed JARs.
- Source scope: `module.reginth/src/main/java/com/altnoir/abysslib/reginth/`.
  The `LICENSE` in that directory identifies the covered files and the
  independently authored MIT-licensed integration files.

Changes include relocation from `com.tterrag.registrate` to
`com.altnoir.abysslib.reginth`, renaming Registrate classes to Reginth names,
removal of generated-code annotations and delombok headers, Minecraft/NeoForge
API adaptations, and integration with AbyssLib's registration and creative-tab
APIs. Modified Registrate-derived source files remain licensed under MPL-2.0.

### Obtaining the corresponding modified source

Every Reginth binary JAR includes the exact registration-framework Java sources
used for that build, together with their scope notice, under:

`META-INF/sources/com/altnoir/abysslib/reginth/`

Open the JAR with a ZIP tool to read or extract these files. For the aggregate
AbyssLib JAR, first open the nested Reginth JAR under `META-INF/jarjar/`, then
open its `META-INF/sources/` directory. No separate download is required.

The complete Reginth module sources and resources are also built and published
as the `sources` classifier of the same Maven group, artifact, and version as
the binary: `com.altnoir:abysslib-reginth-26.1.2:<version>:sources`.
Keep the source artifact with the same version when mirroring releases.

Project source repository: https://github.com/Altnoir/AbyssLib
Minecraft 26.1.2 branch:
https://github.com/Altnoir/AbyssLib/tree/26.1.2-NeoForge

The embedded source is the version-specific source reference for each binary;
the tip of the repository branch can change after a release. Redistribution must
preserve the MPL-covered sources, their license, and a way for recipients to
obtain the sources, as required by MPL-2.0 sections 3.1 and 3.2.

## Athena / ReLink

- Upstream: https://github.com/terrarium-earth/Athena
- Authors: Terrarium Earth and the Athena contributors.
- Upstream copyright: Copyright (c) 2024 Terrarium Earth.
- Source lineage: Athena's Minecraft 1.21.1 implementation, carried through
  AbyssLib's 1.21.1 ReLink module and adapted to Minecraft 26.1.2. The original
  import did not record an exact upstream commit or release number.
- License: MIT (`MIT`).
- Complete original copyright and permission notice:
  `LICENSES/Athena-MIT.txt` in this repository;
  `META-INF/licenses/Athena-MIT.txt` in distributed JARs.
- Source scope: the Athena-derived model loading, connected-texture, model,
  quad, and platform-adapter implementation under
  `module.relink/src/main/java/com/altnoir/abysslib/model/`.
  That directory contains an additional `LICENSE` notice.

Changes include relocation from `earth.terrarium.athena` into AbyssLib's model
packages, renaming Athena API classes to AL names, the `relink:` namespace,
compatibility with Athena-format resources, Minecraft 26.1.2 rendering API
adaptations, and AbyssLib's emissive/model-attribute extensions. Independently
authored AbyssLib additions retain the root MIT license. The original Athena
copyright and permission notice must accompany redistributed copies or
substantial portions of the Athena-derived implementation.

## Release contents

Each binary JAR and each `sources.jar` contains `META-INF/LICENSE`,
`META-INF/THIRD_PARTY_NOTICES.md`, and the license texts in
`META-INF/licenses/`. Sources JARs also retain the directory-level source
license notices. The aggregate JAR preserves these files inside its nested
module JARs.
