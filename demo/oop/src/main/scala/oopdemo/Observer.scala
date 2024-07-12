package oopdemo

trait Observer:
  def signal(e: Event): Unit

trait Event
