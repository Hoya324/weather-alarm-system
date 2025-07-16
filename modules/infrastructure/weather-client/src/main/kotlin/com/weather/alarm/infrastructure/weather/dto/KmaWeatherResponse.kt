package com.weather.alarm.infrastructure.weather.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class KmaWeatherResponse(
    @JsonProperty("response")
    val response: Response
) {
    data class Response(
        @JsonProperty("header")
        val header: Header,
        @JsonProperty("body")
        val body: Body?
    )

    data class Header(
        @JsonProperty("resultCode")
        val resultCode: String,
        @JsonProperty("resultMsg")
        val resultMsg: String
    )

    data class Body(
        @JsonProperty("dataType")
        val dataType: String,
        @JsonProperty("items")
        val items: Items?,
        @JsonProperty("numOfRows")
        val numOfRows: Int,
        @JsonProperty("pageNo")
        val pageNo: Int,
        @JsonProperty("totalCount")
        val totalCount: Int
    )

    data class Items(
        @JsonProperty("item")
        val item: List<Item>
    )

    data class Item(
        @JsonProperty("baseDate")
        val baseDate: String,
        @JsonProperty("baseTime")
        val baseTime: String,
        @JsonProperty("category")
        val category: String,
        @JsonProperty("fcstDate")
        val fcstDate: String?,
        @JsonProperty("fcstTime")
        val fcstTime: String?,
        @JsonProperty("fcstValue")
        val fcstValue: String?,
        @JsonProperty("nx")
        val nx: Int,
        @JsonProperty("ny")
        val ny: Int,
        @JsonProperty("obsrValue")
        val obsrValue: String?
    )
}
