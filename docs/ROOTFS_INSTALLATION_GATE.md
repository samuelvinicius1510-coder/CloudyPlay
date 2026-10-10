# RootFS installation gate

CloudyPlay must not install or launch the Windows runtime until every prerequisite below is true.

## Implemented
- Private workspace creation under `filesDir/windows-runtime`.
- Storage and required-asset preflight.
- Zstandard/TAR reader dependencies.
- Read-only archive validation for unsafe paths, duplicate entries, unsafe links, entry-count limits, and declared expanded-size limits.
- CI compilation of the validator fix.

## Still required before extraction
1. Package or securely obtain the upstream runtime assets and verify their licensing/notice requirements.
2. Validate each archive in the exact install set, not just `rootfs.tzst`.
3. Implement a transactional extractor that preserves Linux symlinks and executable permissions while preventing writes through symlinks outside the staging root.
4. Apply upstream rootfs patches and write the matching version marker only after every step succeeds.
5. Keep the previous valid RootFS until the new staged RootFS passes post-install checks; roll back on failure.
6. Add instrumentation tests with malicious TAR fixtures (absolute paths, traversal, escaping symlinks, duplicate entries, truncated Zstandard streams, and oversized declarations).
7. Integrate and initialize upstream JNI/native components; extraction alone is not a runnable Winlator runtime.

## Why extraction is not enabled yet

The upstream Winlator extractor writes archive entries directly into the destination and creates symlinks. CloudyPlay's installer needs stronger path/symlink protections and a staging/rollback transaction before reusing that behavior. The upstream installer also updates RootFS and container version metadata after extraction; these steps must stay consistent.
