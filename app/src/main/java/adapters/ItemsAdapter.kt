package adapters

import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.documentfile.provider.DocumentFile
import androidx.recyclerview.widget.RecyclerView
import java.lang.invoke.VarHandle

class ItemsAdapter(var files: List<DocumentFile>, private val onComposeItem : @Composable (DocumentFile) -> Unit) : RecyclerView.Adapter<ItemsAdapter.ComposeViewHolder>() {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ComposeViewHolder {
        return ComposeViewHolder(ComposeView(parent.context))
    }



    public fun updateList(updatedFiles: List<DocumentFile>) {
        files = updatedFiles
        notifyItemRangeChanged(0,updatedFiles.size)
    }

    override fun onBindViewHolder(holder: ComposeViewHolder, position: Int) {
        holder.composeView.setContent {
           onComposeItem(files[position])
        }
    }

    override fun getItemCount(): Int {
        return files.size
    }

    class ComposeViewHolder(val composeView: ComposeView) : RecyclerView.ViewHolder(composeView) {

          init {
              // CRITICAL: Prevents disposing the composition until the ViewTree is destroyed.
              // This is key for RecyclerView performance.
              composeView.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
          }
    }
}