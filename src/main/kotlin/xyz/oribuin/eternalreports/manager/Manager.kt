package xyz.oribuin.eternalreports.manager

import xyz.oribuin.eternalreports.EternalReports

abstract class Manager(protected val plugin: EternalReports) {
    abstract fun reload()

    abstract fun disable()
}