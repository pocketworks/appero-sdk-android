# Current Todos & Priorities

## ✅ Recently Completed
- ~~Keyboard is pushing the bottom sheet off the screen when it appears~~ - **FIXED** with Medium article approach
- ~~Add offline feedback handling to our tasks lists~~ - **COMPLETED** with iOS-parity retry system
- ~~Add theming system to match iOS SDK~~ - **COMPLETED** with full theming support

## 🎯 High Priority - Next Sprint

### 📚 Documentation (Critical for SDK adoption)
- **Create comprehensive README.md** - Installation guide, API reference, integration examples
- **Add KDoc documentation** - Document all public APIs with usage examples
- **Create integration samples** - MVVM, Traditional Views, Custom theming examples

### 🧹 Code Cleanup & Architecture
- **Reorganize package structure** - Better separation of concerns (core, ui, network, offline)
- **Remove unused code** - Clean up HelloWorld.kt, unused imports, experimental code
- **Standardize naming conventions** - Consistent API patterns and visibility modifiers

## 🔧 Technical Debt
- Check Retrofit and HTTP Setup Versions - Review for latest stable versions
- Improvements on Backend - When not sending the userID that API doesn't complain
- **Expand unit test coverage** - Target >90% coverage, add UI tests
- **Performance optimization** - Review Compose recomposition, network efficiency

## 🚀 Production Readiness
- **Configure for Maven Central publication** - Move from Maven Local to public distribution
- **Set up CI/CD pipeline** - Automated testing and releases
- **Security review** - API key storage, user data handling, network security
- **Final iOS parity review** - Ensure complete feature and behavior consistency

## 🎨 UI/UX Enhancements
- **Accessibility support** - Screen readers, focus management, content descriptions
- **Animation improvements** - Smooth transitions, loading states
- **Error state handling** - Better user feedback for network issues

## 📱 Future Considerations
- **Flutter plugin preparation** - Design API surface for cross-platform compatibility
- **Analytics integration** - Third-party analytics delegate system (waiting on priority)
- **Traditional View support** - DialogFragment implementation for non-Compose apps

---

## 📋 Status Summary
- **Core SDK**: ✅ Complete (initialization, experience tracking, feedback collection)
- **Offline Support**: ✅ Complete (iOS-parity retry system with 3-minute intervals)
- **UI Components**: ✅ Complete (Jetpack Compose with theming system)
- **Keyboard Handling**: ✅ Complete (Medium article approach with ViewTreeObserver)
- **Documentation**: ❌ Missing (critical for adoption)
- **Code Quality**: ⚠️ Needs attention (cleanup and organization required)
- **Production Ready**: ❌ Not yet (needs Maven Central, CI/CD, security review)