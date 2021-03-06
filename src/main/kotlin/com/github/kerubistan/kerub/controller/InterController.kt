package com.github.kerubistan.kerub.controller

import java.io.Serializable

interface InterController {
	fun sendToController(controllerId: String, msg: Serializable)
	fun broadcast(msg: Serializable)
}