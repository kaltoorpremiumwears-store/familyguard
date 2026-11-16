package org.familyguard.parent.data.model

data class GeneratePairingRequest(
    val childId: String
)

data class PairingResponse(
    val pairingId: String,
    val pairingCode: String,
    val pairingToken: String,
    val qrPayload: String,
    val expiresAt: String,
    val child: ChildSummaryDto
)

data class ChildSummaryDto(
    val id: String,
    val name: String
)

data class PairingStatusResponse(
    val id: String,
    val status: String, // PENDING, ACTIVE, EXPIRED
    val expiresAt: String,
    val isExpired: Boolean,
    val pairedDevice: PairedDeviceDto?
)

data class PairedDeviceDto(
    val id: String,
    val deviceName: String,
    val model: String,
    val isOnline: Boolean,
    val deviceOwnerEnrolled: Boolean
)
