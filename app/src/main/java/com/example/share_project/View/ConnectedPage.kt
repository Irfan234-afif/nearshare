package com.example.share_project.View

import android.app.Activity
import android.content.ContentResolver
import android.net.Uri
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.share_project.DeviceConnectedModel
import com.example.share_project.MainActivity
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectedPage(
    activity: MainActivity,
    deviceConnectedModel: DeviceConnectedModel?,
    sendedPayloadId: Long?,
    transferUpdate: PayloadTransferUpdate?,
    onSelectedFile: (File) -> Unit?
){
    var pickedFileName by remember { mutableStateOf<File?>(null) }
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    var showBottomSheet by remember { mutableStateOf(false) }
    val payloadId by remember {
        mutableStateOf<Long?>(null)
    }
    val pickFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            pickedFileName = getUri(it, activity).path?.let { it1 -> File(it1) }
            if(pickedFileName != null){

            onSelectedFile(pickedFileName!!)
            Log.d("ConnectedPage", "ConnectedPage: Picked File $pickedFileName")
            scope.launch {
                sheetState.show()
            }
            }
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(title = {
                Text(text = "Communicate")
            })
        }
    ) { innerPadding ->
        if(pickedFileName != null){
            ModalBottomSheet(
                sheetState = sheetState,
                onDismissRequest = {
                    showBottomSheet = false
                    pickedFileName = null
                }) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                        .padding(bottom = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Mengirim File",
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Picked file: ${pickedFileName!!.name}",
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    if (transferUpdate != null){
                        
                    LinearProgressIndicator(progress = (transferUpdate.bytesTransferred.toFloat() / transferUpdate.totalBytes.toFloat()))
                        if (transferUpdate.status == PayloadTransferUpdate.Status.SUCCESS){
                            Text(text = "File Berhasil di kirim", color = Color.Green, fontWeight = FontWeight.Bold)
                        }
                    }else{
                        LinearProgressIndicator()
                    }
                }

            }
        }
        Column (modifier = Modifier
            .padding(innerPadding)
            .padding(16.dp)){
            ListItem(headlineContent = { Text(text = deviceConnectedModel?.name ?: "") })
            Button(onClick = {
                pickFileLauncher.launch("*/*")
            }) {
                Text(text = "Choose File")
            }
        }

//    }
    }
}
private fun getUri(uri: Uri, activity: Activity): Uri {
    var resultURI = uri
    if (ContentResolver.SCHEME_CONTENT == uri.scheme) {
        val cr: ContentResolver = activity.baseContext.contentResolver
        val mimeTypeMap = MimeTypeMap.getSingleton()
        val extensionFile = mimeTypeMap.getExtensionFromMimeType(cr.getType(uri))
        val file = File.createTempFile("myTempFile", ".$extensionFile", activity.cacheDir)
        val input = cr.openInputStream(uri)
        file.outputStream().use { stream ->
            input?.copyTo(stream)
        }
        input?.close()
        resultURI = Uri.fromFile(file)
    }
    return resultURI
}
