package dev.brahmkshatriya.echo.ui.player.more.lyrics.applemusic

import android.util.Log

/**
 * Levenshtein edit-distance between two character sequences.
 *
 * Adapted from SimpMusic's StringMatcher.kt (MIT, Copyright (c) maxrave-dev).
 * The algorithm is a standard two-row DP that uses O(len(lhs)) space.
 */
fun levenshtein(
    lhs: CharSequence,
    rhs: CharSequence,
): Int {
    val lhsLength = lhs.length
    val rhsLength = rhs.length

    var cost = IntArray(lhsLength + 1) { it }
    var newCost = IntArray(lhsLength + 1) { 0 }

    for (i in 1..rhsLength) {
        newCost[0] = i

        for (j in 1..lhsLength) {
            val editCost = if (lhs[j - 1] == rhs[i - 1]) 0 else 1

            val costReplace = cost[j - 1] + editCost
            val costInsert = cost[j] + 1
            val costDelete = newCost[j - 1] + 1

            newCost[j] = minOf(costInsert, costDelete, costReplace)
        }

        val swap = cost
        cost = newCost
        newCost = swap
    }

    return cost[lhsLength]
}

/**
 * Returns the index of the closest match in [list] for [s], or null if no match is close enough
 * (edit distance < 20).
 */
fun bestMatchingIndex(
    s: String,
    list: List<String>,
): Int? {
    val listCost = ArrayList<Int>()
    for (i in list.indices) {
        listCost.add(levenshtein(s, list[i]))
    }
    Log.d("Lyrics", "Best cost " + listCost.minOrNull().toString())
    val min = listCost.minOrNull()
    return if (min != null && min < 20) listCost.indexOf(min) else null
}

/**
 * Returns up to 4 indices of the closest matches in [list] for [s], ordered by increasing
 * edit distance.
 */
fun get3MatchingIndex(
    s: String,
    list: List<String>,
): ArrayList<Int> {
    val listIndex = ArrayList<Int>()
    val listCost = ArrayList<Int>()
    for (i in list.indices) {
        listCost.add(levenshtein(s, list[i]))
    }
    listIndex.add(listCost.indexOf(listCost.minOrNull()))
    var count = 1
    while (count <= 3) {
        listCost.clear()
        for (i in list.indices) {
            if (listIndex.contains(i)) continue
            listCost.add(levenshtein(s, list[i]))
        }
        listIndex.add(listCost.indexOf(listCost.minOrNull()))
        count++
    }
    return listIndex
}
