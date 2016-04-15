package tryp
package droid

trait All
extends state.All
with FragmentManagement.ToFragmentManagementOps
with ToIntentOps
with AndroidExt

@integrate(state, state.service)
object `package`
