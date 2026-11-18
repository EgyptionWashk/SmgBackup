# Google Play declaration notes — All files access

This is an internal release-preparation document. It is not displayed in the app and is not a guarantee of Play approval.

## Current implementation status

The current implementation traverses the shared-storage root via `Environment.getExternalStorageDirectory()` to list arbitrary files. That behavior is the reason the manifest currently declares `MANAGE_EXTERNAL_STORAGE`. If the product changes to user-selected folders through the Storage Access Framework, remove the broad permission before release. Play approval is not guaranteed merely because the product is a backup utility.

For user-selected folders, prefer the Storage Access Framework (`ACTION_OPEN_DOCUMENT_TREE`) and persist URI permissions. This is the preferred route for user-directed file backup and normally avoids all-files access.

## If broad access is later genuinely required

1. Confirm the app's core purpose and implemented feature actually require broad access; do not claim eligibility merely because the app is called a backup app.
2. Review the current Google Play policy for `MANAGE_EXTERNAL_STORAGE` and its permitted use cases immediately before submission; policy can change.
3. In Play Console, complete the All files access permission declaration and provide a concise feature description, a demonstration video, and test credentials/instructions if requested.
4. Demonstrate the exact in-app flow that needs broad access and explain why SAF or app-specific storage cannot satisfy it.
5. Keep the permission request contextual and provide a functional experience if the user declines.
6. Do not submit until the implemented feature, manifest, privacy policy, Data safety form, and declaration all agree.

## Important

A backup utility is not automatically approved for all-files access. Google Play makes the final eligibility decision. This document is guidance for engineering/release preparation, not legal or policy approval.

## QUERY_ALL_PACKAGES declaration

The current app also declares `QUERY_ALL_PACKAGES` because it displays installed user apps for backup inventory. This is a separately restricted permission on Google Play. If the installed-app inventory is not essential to the app's core function, remove the permission and use package visibility `<queries>` declarations for only the specific packages that must be discovered. If it is essential, complete the relevant Play Console declaration and explain the user-facing feature.
