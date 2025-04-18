package io.milk.start

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Delivery
import io.ktor.utils.io.*
import io.milk.products.ProductService
import io.milk.products.PurchaseInfo
import io.milk.rabbitmq.ChannelDeliverCallback
import org.slf4j.LoggerFactory

class SaferProductUpdateHandler(private val service: ProductService) : ChannelDeliverCallback {
    private val logger = LoggerFactory.getLogger(this.javaClass)
    private val mapper = ObjectMapper().registerKotlinModule()
    private var channel: Channel? = null

    override fun setChannel(channel: Channel) {
        this.channel = channel
    }

    override fun handle(consumerTag: String, message: Delivery) {
        val purchase = mapper.readValue<PurchaseInfo>(message.body)

        logger.info(
            "received event. purchase for {}, quantity={}, product_id={}",
            purchase.name,
            purchase.amount,
            purchase.id
        )

        try {
            service.decrementBy(purchase)

            // TODO - MESSAGING - can we prevent a failure here?
            //  randomly throw an exception for bacon
            //  ensure the testBestCase test passes
            randomlyThrowAnExceptionForBacon(purchase.name)

            channel!!.basicAck(message.envelope.deliveryTag, true)

        } catch (e: Exception) {
            e.printStack()
            channel!!.basicReject(message.envelope.deliveryTag, true)
        }
    }

    private fun randomlyThrowAnExceptionForBacon(name: String) {
        if ((1..2).random() == 1 && name == "bacon") {
            throw Exception("shoot, something bad happened.")
        }
    }
}