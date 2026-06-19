FLAPPIER-BIRDS

A modern, polished Android arcade game inspired by classic flap-and-dodge gameplay. The player controls a bird that must flap through endless obstacle gaps while collecting rewards, unlocking cosmetic content, achieving high scores, and competing on leaderboards. The game should be lightweight, highly optimized, visually appealing, addictive, and suitable for Android phones and tablets.

REQUIREMENTS

Platform:

* Android App
* Minimum Android 8.0+
* Target latest Android SDK
* Kotlin preferred
* Android Studio compatible project
* Material Design UI
* Dark centered theme throughout
* Responsive layout for phones and tablets
* No UI elements cut off by navigation bars or gesture areas
* Professional polished appearance

Gameplay:

* Endless side-scrolling gameplay
* Tap screen to flap
* Gravity-based flight physics
* Increasing difficulty over time
* Randomized obstacle generation
* Smooth animations
* Particle effects
* Background parallax scrolling
* Day and night cycle
* Dynamic weather effects (optional rain, clouds, fog)
* Pause and resume support
* Sound effects
* Background music with mute controls
* Haptic feedback option
* Offline playable

Game Features:

* Local high score system
* Statistics page
* Achievement system
* Unlockable bird skins
* Unlockable backgrounds
* Coin collection system
* Daily rewards
* Multiple bird characters
* Challenge mode
* Endless mode
* Practice mode
* Best score tracking
* Session statistics

Menus:

* Main Menu
* Play
* Statistics
* Achievements
* Settings
* About
* Exit

Settings:

* Music volume
* Sound volume
* Haptic feedback toggle
* Graphics quality
* FPS display
* Debug mode toggle

About Section:

* Display:
  Made by jnetai.com
* Display full version number
* Version must match GitHub release tag exactly
* Check For Updates button
* Share App button
* Show latest available release version
* Internet permission enabled for update checks

Persistence:

* Save all game settings
* Save statistics
* Save unlocked content
* Save achievements
* Save best scores
* Restore automatically on launch

Leaderboard:

* Local leaderboard
* Architecture prepared for future online leaderboard support

Performance:

* Optimized for Raspberry Pi Android devices and older phones
* Stable 60 FPS target
* Low battery usage
* Efficient memory management
* No memory leaks

Debugging And Diagnostics:

* Permanent logging system
* Permanent debug subsystem
* Permanent validation checks
* Error tracking framework
* Unique error codes
* Detailed stack traces
* Diagnostic reports
* Crash recovery information
* Startup diagnostics
* Runtime diagnostics
* Asset validation
* Save-file validation
* Network diagnostics
* Keep debugging systems permanently integrated

Versioning:

* Semantic versioning
* Version displayed throughout app
* Version synchronized with GitHub release tags

GitHub Workflows:

* Build APK entirely using GitHub Actions
* No local builds required
* Automatically build on push
* Automatically build on release
* Automatically upload APK artifact
* Automatically create GitHub release assets
* Store final APK in:
  apk/
* Release APK filename example:
  Flappier-Birds.apk
* Do not create separate debug APK releases
* Main APK should be the release APK

Signing:

* Use same keystore for all releases
* Maintain update compatibility
* Never generate random signing keys between releases
* Read signing credentials from GitHub Secrets
* Configure workflow for long-term upgrade compatibility

Repository Structure:

* app/
* apk/
* docs/
* .github/workflows/
* assets/
* changes.txt

LNKS / REFERENCES

Gameplay Inspiration:

* Classic endless flap-style obstacle avoidance games
* Material Design guidelines
* Android developer best practices
* GitHub Actions Android build workflows

PROJECT LOCATION PATH

/home/jay/Documents/Scripts/AI/OpenCode/flappier-birds/

EXPECTATIONS

* Produce production-quality code
* No placeholder screens
* No unfinished systems
* No TODO markers left behind
* Professional visual design
* Smooth gameplay
* Stable releases
* Fully functional APK generation
* Complete Android project structure
* Complete GitHub Actions workflow
* Complete signing configuration
* Complete update checking system
* Complete About page
* Complete persistence systems
* Complete diagnostics framework
* Maintain readable codebase
* Maintain changelog history
* Update changes.txt whenever files are added or modified
* Never edit Backup folders
* Use Backup folders only as read-only references if they exist
* Save all file changes
* Ensure project remains buildable at all times

use github workflows to build the app and put final release APK into the apk folder in the project location

Dont edit this file

Never change anything in Backup folders (if it exists) but you can use them as a read-only reference if a mistake is made and you need to fix something

save changes to file(s) in question

then after files are added / edited then save any changes made to changes.txt

github api tokens / passwords etc can be found in:
/home/jay/Documents/Scripts/AI/openclaw/password-vault/

always use the same keystore for each app made via github workflows so the application can update correctly without requiring uninstall

Save changes to changes.txt (create if not exists)

tell me when ready to test and remain silent until fully complete, only responding if input is required, an update is requested, or the project is ready for testing

when providing the final GitHub release URL, link to the latest release page rather than directly to the APK file


