package com.cloudlinks.aimeebadminton.referee

enum class Side { YOU, AIMEE }

data class MatchScore(
    val you: Int = 0,
    val aimee: Int = 0,
    val server: Side = Side.YOU,
    val finished: Boolean = false,
    val winner: Side? = null,
    val gamesYou: Int = 0,
    val gamesAimee: Int = 0
)

/**
 * Standard BWF rally-point scoring for a singles game.
 * The vision pipeline must submit a confirmed rally winner; this class never
 * guesses a point from a missing detection.
 */
class ScoreEngine {
    var score: MatchScore = MatchScore()
        private set

    fun start(firstServer: Side = Side.YOU) {
        score = MatchScore(server = firstServer)
    }

    fun point(winner: Side): MatchScore {
        if (score.finished) return score
        var you = score.you
        var aimee = score.aimee
        if (winner == Side.YOU) you++ else aimee++

        val high = maxOf(you, aimee)
        val diff = kotlin.math.abs(you - aimee)
        val finished = (high >= 21 && diff >= 2) || high == 30
        val nextServer = winner
        score = score.copy(
            you = you,
            aimee = aimee,
            server = nextServer,
            finished = finished,
            winner = if (finished) winner else null
        )
        return score
    }

    fun undo(): MatchScore {
        // Deliberately handled by the UI's short event history in a later milestone.
        return score
    }
}
