

## Recent UI and import updates

The folder-to-album import now preserves the exact folder name returned by the Android system picker. The app no longer adds numeric suffixes such as `(2)` when creating an album from a folder; if an album with the same name already exists, the repository reports the duplicate instead of silently renaming the folder.

The Lock and Home screen enable cards, previously the first items inside **Advanced**, are now the first main options above the current wallpaper preview. The remaining scheduling, appearance, and effect controls stay inside **Advanced**.

A language selector is available in Settings with **System default**, **English**, and **العربية**. Selecting Arabic applies the app locale and recreates the Activity so the Arabic resources take effect immediately. The main settings and wallpaper controls include Arabic translations, while less frequently used strings safely fall back to English.


## Folder import reliability update

The folder importer now preserves the exact name returned by Android's system folder picker. Tree URIs are resolved through the tree document provider instead of the single-document API, with a document-id fallback for OEM providers. Multiple folders can now be imported, including folders with identical names; each receives an independent album identifier and no automatic numeric suffix is added. The app persists read permission for each selected tree so scheduled wallpaper changes can continue to access all imported folders.
