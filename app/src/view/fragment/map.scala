// package tryp
// package droid

// import com.google.android.gms.maps.{MapFragment => AMapFragment,_}

// import macroid.Contexts

// case class MapFragment()
// extends AMapFragment
// with tryp.droid.FragmentBase
// with Contexts[android.app.Fragment]
// with OnMapReadyCallback
// with DefaultStrategy
// {
//   lazy val showMapActor = actor(ShowMapActor.props)

//   def onMapReady(googleMap: GoogleMap) {
//     showMapActor ! Messages.MapReady(googleMap)
//     val ui = getMap.getUiSettings
//     ui.setRotateGesturesEnabled(false)
//     ui.setTiltGesturesEnabled(false)
//   }

//   override def onResume() {
//     super.onResume()
//     getMapAsync(this)
//   }

//   override def onStart() = super.onStart()
//   override def onStop() = super.onStop()

//   override def onViewStateRestored(state: Bundle) {
//     super.onViewStateRestored(state)
//   }

//   override def onActivityCreated(state: Bundle) {
//     super.onActivityCreated(state)
//   }
//   override def onCreate(state: Bundle) { super.onCreate(state) }
// }
