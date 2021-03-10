package ltd.evilcorp.atox.ui.user_profile

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Bundle
import android.text.InputFilter
import android.view.ContextMenu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.getSystemService
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.viewModels
import ltd.evilcorp.atox.R
import ltd.evilcorp.atox.databinding.FragmentUserProfileBinding
import ltd.evilcorp.atox.ui.BaseFragment
import ltd.evilcorp.atox.ui.StatusDialog
import ltd.evilcorp.atox.vmFactory
import ltd.evilcorp.core.vo.UserStatus

private const val TOX_MAX_NAME_LENGTH = 128
private const val TOX_MAX_STATUS_MESSAGE_LENGTH = 1007

class UserProfileFragment : BaseFragment<FragmentUserProfileBinding>(FragmentUserProfileBinding::inflate) {
    private val vm: UserProfileViewModel by viewModels { vmFactory }
    private lateinit var currentStatus: UserStatus

    private fun colorFromStatus(status: UserStatus) = when (status) {
        UserStatus.None -> ResourcesCompat.getColor(resources, R.color.statusAvailable, null)
        UserStatus.Away -> ResourcesCompat.getColor(resources, R.color.statusAway, null)
        UserStatus.Busy -> ResourcesCompat.getColor(resources, R.color.statusBusy, null)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) = binding.run {
        ViewCompat.setOnApplyWindowInsetsListener(view) { _, compat ->
            val insets = compat.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
            profileCollapsingToolbar.updatePadding(left = insets.left, right = insets.right)
            profileToolbar.updatePadding(top = insets.top)
            mainSection.updatePadding(left = insets.left, right = insets.right)
            compat
        }

        profileToolbar.apply {
            setNavigationOnClickListener {
                activity?.onBackPressed()
            }
        }

        vm.user.observe(viewLifecycleOwner) { user ->
            currentStatus = user.status

            userName.text = user.name
            userStatusMessage.text = user.statusMessage
            userStatus.setColorFilter(colorFromStatus(user.status))
        }

        userToxId.text = vm.toxId.string()
        profileShareId.setOnClickListener {
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, vm.toxId.string())
            }
            startActivity(Intent.createChooser(shareIntent, getString(R.string.tox_id_share)))
        }
        registerForContextMenu(profileShareId)

        profileOptions.profileChangeNickname.setOnClickListener {
            val nameEdit = EditText(requireContext()).apply {
                text.append(binding.userName.text)
                filters = arrayOf(InputFilter.LengthFilter(TOX_MAX_NAME_LENGTH))
                setSingleLine()
            }
            AlertDialog.Builder(requireContext())
                .setTitle(R.string.name)
                .setView(nameEdit)
                .setPositiveButton(R.string.update) { _, _ ->
                    vm.setName(nameEdit.text.toString())
                }
                .setNegativeButton(R.string.cancel) { _, _ -> }
                .show()
        }

        profileOptions.profileChangeStatusText.setOnClickListener {
            val statusMessageEdit =
                EditText(requireContext()).apply {
                    text.append(binding.userStatusMessage.text)
                    filters = arrayOf(InputFilter.LengthFilter(TOX_MAX_STATUS_MESSAGE_LENGTH))
                }
            AlertDialog.Builder(requireContext())
                .setTitle(R.string.status_message)
                .setView(statusMessageEdit)
                .setPositiveButton(R.string.update) { _, _ ->
                    vm.setStatusMessage(statusMessageEdit.text.toString())
                }
                .setNegativeButton(R.string.cancel) { _, _ -> }
                .show()
        }

        profileOptions.profileChangeStatus.setOnClickListener {
            StatusDialog(requireContext(), currentStatus) { status -> vm.setStatus(status) }.show()
        }

        // TODO(robinlinden): Remove hack. It's used to make sure we can scroll to the settings
        //  further down when in landscape orientation. This is only needed if the view is recreated
        //  while we're on this screen as Android changes the size of the contents of the NestedScrollView
        //  when that happens.
        if (savedInstanceState != null) {
            needsHacks.updatePadding(bottom = (150 * resources.displayMetrics.density).toInt())
        }
    }

    override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenu.ContextMenuInfo?) = binding.run {
        super.onCreateContextMenu(menu, v, menuInfo)
        when (v.id) {
            R.id.profile_share_id -> requireActivity().menuInflater.inflate(
                R.menu.user_profile_share_id_context_menu,
                menu
            )
        }
    }

    override fun onContextItemSelected(item: MenuItem): Boolean = binding.run {
        return when (item.itemId) {
            R.id.copy -> {
                val clipboard = requireActivity().getSystemService<ClipboardManager>()!!
                clipboard.setPrimaryClip(ClipData.newPlainText(getText(R.string.tox_id), vm.toxId.string()))
                Toast.makeText(requireContext(), getText(R.string.copied), Toast.LENGTH_SHORT).show()
                true
            }
            else -> super.onContextItemSelected(item)
        }
    }
}
