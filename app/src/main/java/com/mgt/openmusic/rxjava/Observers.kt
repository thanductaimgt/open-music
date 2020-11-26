package com.mgt.openmusic.rxjava

import androidx.annotation.CallSuper
import com.mgt.openmusic.CompositeSubscription
import com.mgt.openmusic.print
import io.reactivex.rxjava3.core.CompletableObserver
import io.reactivex.rxjava3.core.Observer
import io.reactivex.rxjava3.core.SingleObserver
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.disposables.Disposable
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription

abstract class MyObserver(private val compositeDisposable: CompositeDisposable){
    private lateinit var disposable: Disposable

    @CallSuper
    open fun onSubscribe(d: Disposable) {
        setDisposable(d)
    }

    @CallSuper
    open fun onError(t:Throwable) {
        removeDisposable()
        t.print()
    }

    private fun setDisposable(disposable: Disposable){
        this.disposable = disposable
        compositeDisposable.add(disposable)
    }
    protected fun removeDisposable(){
        if (::disposable.isInitialized) {
            compositeDisposable.remove(disposable)
        }
    }
}

open class MySingleObserver<T>(compositeDisposable: CompositeDisposable) :
    MyObserver(compositeDisposable),
    SingleObserver<T> {
    override fun onSuccess(result: T) {
        removeDisposable()
    }
}

open class MyCompletableObserver(compositeDisposable: CompositeDisposable) :
    MyObserver(compositeDisposable),
    CompletableObserver {
    override fun onComplete() {
        removeDisposable()
    }
}

abstract class MyStreamObserver<T>(compositeDisposable: CompositeDisposable) :
    MyObserver(compositeDisposable),
    Observer<T> {
    override fun onComplete() {
        removeDisposable()
    }
}

abstract class MySubscription(private val compositeSubscription: CompositeSubscription){
    private lateinit var sub:Subscription

    @CallSuper
    open fun onSubscribe(sub:Subscription) {
        setSubscription(sub)
    }

    @CallSuper
    open fun onError(t:Throwable) {
        removeSubscription()
        t.print()
    }

    private fun setSubscription(sub:Subscription){
        this.sub = sub
        compositeSubscription.add(sub)
    }
    protected fun removeSubscription(){
        if (::sub.isInitialized) {
            compositeSubscription.remove(sub)
        }
    }
}

abstract class MyStreamSubscriber<T>(compositeSubscription: CompositeSubscription) :
    MySubscription(compositeSubscription),
    Subscriber<T> {
    override fun onSubscribe(sub: Subscription) {
        super.onSubscribe(sub)
    }

    override fun onComplete() {
        removeSubscription()
    }
}