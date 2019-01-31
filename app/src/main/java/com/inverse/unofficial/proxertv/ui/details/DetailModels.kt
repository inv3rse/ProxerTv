package com.inverse.unofficial.proxertv.ui.details

import com.inverse.unofficial.proxertv.model.Series
import com.inverse.unofficial.proxertv.model.SeriesList

/**
 * Data for the series header information
 */
data class DetailsData(
    val series: Series,
    val seriesList: SeriesList,
    val userProgress: Int,
    val currentPage: Int
)

/**
 * A category with a list of episodes
 */
data class EpisodeCategory(
    val title: String,
    val episodes: List<Int>,
    val progress: Int
)