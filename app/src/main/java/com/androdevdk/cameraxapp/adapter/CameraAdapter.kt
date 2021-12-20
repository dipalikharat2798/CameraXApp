package com.androdevdk.cameraxapp.adapter

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.androdevdk.cameraxapp.R
import com.androdevdk.cameraxapp.model.Image
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions

class CameraAdapter(
    private val context: Context,
    private val mList: List<Image>,
    private val mList1: List<String>
) :
    RecyclerView.Adapter<CameraAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item, parent, false)

        return ViewHolder(view)
    }

    // binds the list items to a view
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val ItemsViewModel = mList[position]
        //  Log.d("position", "onBindViewHolder: "+position)
        var img: String? = ""
        if (!mList1.isEmpty()) {
            img = mList1.get(position)
        }
        if (img != "") {
            Glide.with(holder.imageView)
                .load(img)
                .apply(RequestOptions.circleCropTransform())
                .into(holder.imageView)
        } else {
            holder.imageView.setImageResource(ItemsViewModel.image)
        }

    }

    // return the number of the items in the list
    override fun getItemCount(): Int {
        return mList.size
    }

    // Holds the views for adding it to image and text
    class ViewHolder(ItemView: View) : RecyclerView.ViewHolder(ItemView) {
        val imageView: ImageView = itemView.findViewById(R.id.imageview)
    }
}
