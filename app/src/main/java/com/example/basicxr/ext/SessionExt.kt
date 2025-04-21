@file:Suppress("SpellCheckingInspection")

package com.example.basicxr.ext

import android.util.Log
import androidx.concurrent.futures.await
import androidx.xr.scenecore.GltfModel
import androidx.xr.scenecore.Model
import androidx.xr.scenecore.Session


suspend fun Session.loadGltfModel(assetName: String): Model? {
    Log.d("modelss =SessionExt", "Loading GLTF model from URI: $assetName")

    val loadedGltfModel = try {
        GltfModel.create(this, assetName).await()
    } catch (e: Exception) {
        Log.e("modelss =SessionExt", "Error loading model $assetName: ${e.message}", e)
        return null
    }

    Log.i("modelss =SessionExt", "Model $assetName loaded successfully")
    return loadedGltfModel
}