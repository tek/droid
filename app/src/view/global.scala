package tryp
package droid

trait ViewExports
extends FragmentManagement.ToFragmentManagementOps
with ToSearchable
with ToSearchView
with HasContextF.ToHasContextFOps
with HasActivityF.ToHasActivityFOps
with RootView.ToRootViewOps

object ViewExports
extends ViewExports
