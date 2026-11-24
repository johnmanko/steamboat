# Changelog

## 1.0.0

First release. Android 10 (API 29) and newer.

### Messaging

- Default SMS app, with the existing message history imported into the inbox.
- One-to-one and group threads, contact photos, and unread counts.
- Search across message text. Each match is its own row, newest first, with the term highlighted, and opens on that message.
- Photo and video attachments, inline previews, and a full-screen viewer. A photo, video, or group message is sent as MMS.
- Links in a message open in the browser.
- Location sharing. The map button inserts an OpenStreetMap or Google Maps link at the cursor.
- Quick replies on a new-message notification and in the composer. Saved phrases are 15 characters or less.
- Steamboat reads whether RCS is registered. A text is sent as SMS.

### Notifications

- App-wide choice of silent, sound, vibration, both, or the system default, with the system notification sound.
- Sound and vibration can be limited to part of the day, including a window that runs overnight.
- A conversation can override the app-wide choice. The icon after Messages shows the app-wide setting, and a custom thread shows its own icon in the inbox and under the name.
- Reply and mark-as-read actions on a notification.

### Themes

- Bundled palettes installed into a theme library, each drawn as a three-stop gradient.
- Text is taken from the palette so it stays readable. System follows the device theme.
- A theme whose framework version does not match the app stays hidden until it is updated.
- Steamboat is the theme on a new install. An upgrade keeps the saved theme unless that theme has been removed.

### Settings

- Quick replies, location, notifications, themes, and about.
- About links to the creator, the source repository, and the GPLv3 license.
