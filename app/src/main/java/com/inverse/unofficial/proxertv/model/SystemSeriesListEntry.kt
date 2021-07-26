package com.inverse.unofficial.proxertv.model

data class SystemSeriesListEntry(
    val orderPosition: Long,
    val listType: SystemSeriesList,
    val seriesId: Long,
    val seriesName: String
) : ISeriesCover {

    override val id: Long
        get() = seriesId

    override val name: String
        get() = seriesName
}