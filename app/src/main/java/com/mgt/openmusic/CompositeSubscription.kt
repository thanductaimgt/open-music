package com.mgt.openmusic

import org.reactivestreams.Subscription

class CompositeSubscription {
    private val subs = HashSet<Subscription>()

    fun add(sub:Subscription){
        subs.add(sub)
    }

    fun remove(sub: Subscription){
        subs.remove(sub)
    }

    fun clear(){
        for(sub in subs){
            sub.cancel()
        }
        subs.clear()
    }
}