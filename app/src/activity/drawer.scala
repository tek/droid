//   abstract override def onPostCreate(state: Bundle) {
//     super.onPostCreate(state)
//     syncToggle()
//   }

//   def syncToggle() {
//     drawerToggle <<~ D.sync
//   }

//   abstract override def onConfigurationChanged(newConf: Configuration) {
//     super.onConfigurationChanged(newConf)
//     drawerToggle foreach { _.onConfigurationChanged(newConf) }
//   }

//   def drawerOpen = drawer exists { _.isDrawerOpen(Gravity.LEFT) }

//   def closeDrawer = drawer <~ D.close()

//   def openDrawer = drawer <~ D.open()

//   override def contentLoaded() {
//     updateToggle()
//   }

//   def updateToggle() {
//     if (canGoBack) enableBackButton()
//     else syncToggle()
//   }

//   def enableBackButton() {
//     drawerToggle foreach { _.onDrawerOpened(null) }
//   }

//   def onDrawerOpened(drawerView: View) {
//     drawerToggle foreach { _.onDrawerOpened(drawerView) }
//     state ! DrawerOpened
//   }

//   def onDrawerClosed(drawerView: View) {
//     drawerToggle foreach { _.onDrawerClosed(drawerView) }
//     state ! DrawerClosed(updateToggle)
//   }

//   def onDrawerSlide(drawerView: View, slideOffset: Float) {
//     drawerToggle foreach { _.onDrawerSlide(drawerView, slideOffset) }
//   }

//   def onDrawerStateChanged(newState: Int) {
//     drawerToggle foreach { _.onDrawerStateChanged(newState) }
//   }

//   override def navButtonClick() = {
//     if (drawerOpen) closeDrawer.run
//     else if (!super.navButtonClick()) openDrawer.run
//     true
//   }

//   def closeIfOpen() = {
//     if (drawerOpen) closeDrawer.run
//   }

//   def drawerClick(action: droid.state.Message) = {
//     closeIfOpen()
//     send(action)
//   }
// }
