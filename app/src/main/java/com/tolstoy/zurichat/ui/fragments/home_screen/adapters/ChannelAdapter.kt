package com.tolstoy.zurichat.ui.fragments.home_screen.adapters

import android.app.Activity
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import centrifuge.Client
import centrifuge.PublishEvent
import centrifuge.Subscription
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.tolstoy.zurichat.R
import com.tolstoy.zurichat.data.localSource.AppDatabase
import com.tolstoy.zurichat.models.Channel
import com.tolstoy.zurichat.models.ChannelModel
import com.tolstoy.zurichat.ui.fragments.channel_chat.localdatabase.RoomDao
import com.tolstoy.zurichat.ui.fragments.channel_chat.localdatabase.RoomDataObject
import com.tolstoy.zurichat.ui.fragments.home_screen.CentrifugeClient
import com.tolstoy.zurichat.ui.fragments.home_screen.CentrifugeClient.CustomListener
import com.tolstoy.zurichat.ui.fragments.networking.JoinNewChannel
import com.tolstoy.zurichat.ui.fragments.networking.RetrofitClientInstance
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ChannelAdapter(val context: Activity, private val list: List<ChannelModel>,val uiScope: CoroutineScope,val roomDao: RoomDao) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private var onItemClickListener: ((channel:ChannelModel) -> Unit)? = null
    private var onAddChannelClickListener: (() -> Unit)? = null
    lateinit var organizationId : String
    lateinit var database: AppDatabase

    fun setItemClickListener(listener: (channel:ChannelModel) -> Unit) {
        onItemClickListener = listener
    }

    fun setAddChannelClickListener(listener: () -> Unit) {
        onAddChannelClickListener = listener
    }

    inner class ChannelViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        fun bind(channel: ChannelModel) {
            val fab = view.findViewById<FloatingActionButton>(R.id.fab)
            val badge = view.findViewById<MaterialButton>(R.id.badge)
            badge.visibility = View.GONE

            if (!channel.isPrivate){
                fab.setImageDrawable(ContextCompat.getDrawable(context,R.drawable.ic_hash))
            }else{
                fab.setImageDrawable(ContextCompat.getDrawable(context,R.drawable.ic_new_lock))
            }

            view.findViewById<TextView>(R.id.channelTitle).text = channel.name
            view.findViewById<ConstraintLayout>(R.id.root_layout).setOnClickListener {
                onItemClickListener?.invoke(channel)
            }

            var count = 0
            uiScope.launch(Dispatchers.IO){
                roomDao.getRoomDataWithChannelID(channel._id).let { roomDataObject ->
                    if (roomDataObject!=null){
                        try{
                            CentrifugeClient.setCustomListener(object : CustomListener {
                                override fun onConnected(connected: Boolean) {
                                    CentrifugeClient.subscribeToChannel(roomDataObject.socketName)
                                }
                                override fun onDataPublished(subscription: Subscription?, publishEvent: PublishEvent?) {
                                    uiScope.launch(Dispatchers.Main){
                                        count++
                                        badge.text = count.toString()
                                    }
                                }
                            })
                        }catch(e : Exception){
                            e.printStackTrace()
                        }
                    }else{
                        val room = RetrofitClientInstance.retrofitInstance?.create(JoinNewChannel::class.java)?.getRoom(organizationId,channel._id)
                        room?.let {
                            try{
                                CentrifugeClient.setCustomListener(object : CustomListener {
                                    override fun onConnected(connected: Boolean) {
                                        CentrifugeClient.subscribeToChannel(it.socket_name)
                                    }
                                    override fun onDataPublished(subscription: Subscription?, publishEvent: PublishEvent?) {
                                        uiScope.launch(Dispatchers.Main){
                                            count++
                                            badge.text = count.toString()
                                        }
                                    }
                                })

                                val roomDataObject = RoomDataObject()
                                roomDataObject.channelId = it.channel_id
                                roomDataObject.socketName = it.socket_name

                                roomDao.insertAll(roomDataObject)
                            }catch(e : Exception){
                                e.printStackTrace()
                            }
                        }
                    }
                }
            }
        }
    }

    inner class HeaderViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        fun bind(channel: ChannelModel) {
            val nameView = view.findViewById<TextView>(R.id.headerTextView)
            nameView.text = channel.name

            /***
             * Checks if the header is a Unread Message Header Or Add New Channel Header.
             * Removes The Add Icon If it is a Unread Message Header
             * Sets A Click Listener If It is a Add Channel Header
             */
            if (channel.type == "channel_header_add"){
                view.findViewById<ConstraintLayout>(R.id.root_layout).setOnClickListener {
                    onAddChannelClickListener?.invoke()
                }
            }else{
                nameView.setCompoundDrawablesRelativeWithIntrinsicBounds(0,0,0,0)
            }
        }
    }

    inner class DividerViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        fun bind(channel: Channel) {
            val nameView = view.findViewById<View>(R.id.divider)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder{
        val view : View
        val viewHolder: RecyclerView.ViewHolder

        if (viewType == 0){
            view = context.layoutInflater.inflate(R.layout.channel_header_model, parent, false)
            viewHolder = HeaderViewHolder(view)
            return viewHolder
        }else if (viewType == 2){
            view = context.layoutInflater.inflate(R.layout.channel_divider_model, parent, false)
            viewHolder = DividerViewHolder(view)
            return viewHolder
        }
        view = context.layoutInflater.inflate(R.layout.channels_list_item, parent, false)
        viewHolder = ChannelViewHolder(view)
        return viewHolder
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is ChannelViewHolder) {
            holder.bind(list[position])
            //Perform Channel Related Actions Here
        }else if (holder is HeaderViewHolder){
            holder.bind(list[position])
            holder.setIsRecyclable(false)
            //Perform Header Related actions here
        }
    }

    override fun getItemCount() = list.size

    override fun getItemId(position: Int): Long {
        return list[position].hashCode().toLong()
    }

    override fun getItemViewType(position: Int): Int {
        return list[position].viewType
    }

}