/**
* Copyright (c) 2012 Partners In Health.  All rights reserved.
* The use and distribution terms for this software are covered by the
* Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
* which can be found in the file epl-v10.html at the root of this distribution.
* By using this software in any fashion, you are agreeing to be bound by
* the terms of this license.
* You must not remove this notice, or any other, from this software.
**/ 
package org.pih.warehouse.receiving

import grails.validation.ValidationException
import org.pih.warehouse.api.PartialReceipt
import org.pih.warehouse.api.PartialReceiptContainer
import org.pih.warehouse.api.PartialReceiptItem
import org.pih.warehouse.inventory.InventoryItem
import org.pih.warehouse.shipping.Container
import org.pih.warehouse.shipping.Shipment
import org.pih.warehouse.shipping.ShipmentItem

class ReceiptService {

    boolean transactional = true

    def shipmentService
    def inventoryService

    PartialReceipt getPartialReceipt(String id) {
        Shipment shipment = Shipment.get(id)

        PartialReceipt partialReceipt = new PartialReceipt()
        partialReceipt.shipment = shipment
        partialReceipt.recipient = shipment.recipient
        partialReceipt.dateShipped = shipment.actualShippingDate
        partialReceipt.dateDelivered = shipment.actualDeliveryDate

        shipment.containers.collect { Container container ->

            PartialReceiptContainer partialReceiptContainer = new PartialReceiptContainer()
            partialReceiptContainer.container = container
            partialReceipt.partialReceiptContainers.add(partialReceiptContainer)

            container.shipmentItems.collect { ShipmentItem shipmentItem ->
                PartialReceiptItem partialReceiptItem = new PartialReceiptItem()
                partialReceiptItem.shipmentItem = shipmentItem
                partialReceiptContainer.partialReceiptItems.add(partialReceiptItem)
            }
        }
        return partialReceipt
    }

    void savePartialReceipt(PartialReceipt partialReceipt) {

        log.info "Saving partial receipt " + partialReceipt

        Shipment shipment = partialReceipt?.shipment
        Receipt receipt = shipment?.receipt

        if (!receipt)
            receipt = new Receipt()

        // Update receipt header
        receipt.recipient = partialReceipt.recipient
        receipt.shipment = partialReceipt.shipment
        receipt.expectedDeliveryDate = partialReceipt.dateDelivered
        receipt.actualDeliveryDate = partialReceipt.dateDelivered


        // Update receipt items
        partialReceipt.partialReceiptItems.each { partialReceiptItem ->

            log.info "Saving partial receipt item " + partialReceiptItem

            ShipmentItem shipmentItem = partialReceiptItem.shipmentItem
            if (!shipmentItem) {
                throw new IllegalArgumentException("Cannot receive item without valid shipment item")
            }

            InventoryItem inventoryItem =
                    inventoryService.findOrCreateInventoryItem(shipmentItem.product, shipmentItem.lotNumber, shipmentItem.expirationDate)

            if (!inventoryItem) {
                throw new IllegalArgumentException("Cannot receive item without valid inventory item")
            }

            ReceiptItem receiptItem = new ReceiptItem();
            receiptItem.quantityReceived = partialReceiptItem.quantityReceiving
            receiptItem.binLocation = partialReceiptItem.binLocation
            receiptItem.quantityShipped = shipmentItem.quantity;
            receiptItem.lotNumber = shipmentItem.lotNumber;
            receiptItem.product = inventoryItem.product
            receiptItem.inventoryItem = inventoryItem
            receiptItem.shipmentItem = shipmentItem
            receipt.addToReceiptItems(receiptItem)
            shipmentItem.addToReceiptItems(receiptItem)
            receipt.save(flush:true)
        }
        shipment.receipt = receipt
        shipment.save()
    }

    void saveInboundTransaction(PartialReceipt partialReceipt) {
        Shipment shipment = partialReceipt.shipment
        if (shipment) {
            rollbackInboundTransactions(shipment)
            shipmentService.createInboundTransaction(shipment)
        }
    }

    void rollbackInboundTransactions(Shipment shipment) {
        if (shipment.incomingTransactions) {
            shipment.incomingTransactions?.toArray().each {
                shipment.removeFromIncomingTransactions(it)
                it.delete()
            }
        }
    }


    void rollbackPartialReceipts(Shipment shipment) {
        log.info "Rollback partial receipts for shipment " + shipment
        if (!shipment) {
            throw new IllegalArgumentException("Cannot rollback without valid shipment")
        }

        rollbackInboundTransactions(shipment)

        if (shipment.receipt) {
            shipment.receipt?.delete()
            shipment.receipt = null
        }
    }

}
