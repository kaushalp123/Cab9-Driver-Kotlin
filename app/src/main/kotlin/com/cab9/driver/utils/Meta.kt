package com.cab9.driver.utils

const val BALANCE_ZERO = 0.0

const val GRANT_TYPE_PASSWORD = "password"
const val AUTHORIZATION = "Authorization"
const val BEARER = "Bearer"

const val MAX_PAGE_SIZE = 15
const val CAB9_STARTING_PAGE_INDEX = 1
const val MIN_PASSWORD_LENGTH = 6

const val DAYS_FOR_FLEXIBLE_UPDATE = 4

const val IN_APP_NOTIFICATION_TIMEOUT = 5000L

/**
 * Do not change this value. This is being used in [nav_graph#cab9GoWebFragment]
 */
const val BUNDLE_KEY_BOOKING_ID = "bookingId"
const val BUNDLE_KEY_STATUS = "status"
const val BUNDLE_KEY_TYPE = "type"
////////////////////////////////////

const val BUNDLE_KEY_PERMISSION_SHOWN = "permission_shown"

const val RQ_SHOW_ACCEPTED_OFFER = "com.cab9.driver.REQUEST_KEY_SHOW_ACCEPTED_OFFER"
const val EXTRA_ACCEPTED_OFFER_BOOKING_ID = "accepted_booking_id"

const val ACTION_REFRESH_UPCOMING = "com.cab9.driver.ACTION_REFRESH_UPCOMING"
const val ACTION_REFRESH_MOBILE_STATE = "com.cab9.driver.ACTION_REFRESH_MOBILE_STATE"
const val ACTION_BOOKING_DETAIL_UPDATE = "com.cab9.driver.ACTION_BOOKING_DETAIL_UPDATE"
const val ACTION_BOOKING_PRICE_UPDATE = "com.cab9.driver.ACTION_BOOKING_PRICE_UPDATE"
const val ACTION_TEST_BOOKING_OFFER_RECEIVED = "com.cab9.driver.ACTION_TEST_BOOKING_OFFER_RECEIVED"
const val ACTION_TEST_BOOKING_OFFER_EVENT = "com.cab9.driver.ACTION_TEST_BOOKING_OFFER_EVENT"
const val ACTION_QUICK_MESSAGE_SELECTED = "com.cab9.driver.QUICK_MESSAGE_SELECTED"
const val ACTION_QUICK_MESSAGE_DELETED = "com.cab9.driver.QUICK_MESSAGE_DELETED"
const val ACTION_NEW_QUICK_MESSAGE_REQUEST = "com.cab9.driver.NEW_QUICK_MESSAGE_REQUEST"
const val ACTION_NEW_QUICK_MESSAGE_ADDED = "com.cab9.driver.NEW_QUICK_MESSAGE_ADDED"

const val EXTRA_BOOKING_DETAIL = "booking_detail"
const val EXTRA_BOOKING_PRICE = "booking_price"

const val REQUEST_KEY_UPDATE_EXPENSE = "rq_update_expenses"
const val REQUEST_KEY_REFRESH_UPCOMING = "rq_refresh_upcoming"

const val REQUEST_KEY_SUBMIT_AUCTION_BID = "com.cab9.driver.REQUEST_KEY_SUBMIT_AUCTION_BID"
const val BUNDLE_KEY_ENTERED_BID_AMOUNT = "entered_bid_amount"
const val BUNDLE_BID_TYPE = "bid_type"
const val BUNDLE_BID_BOOKING_ID = "bid_booking_id"

const val GOOGLE_DOCS_EMBEDDED_URL = "http://docs.google.com/gview?embedded=true&url="