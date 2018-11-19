package gt.creeperface.nukkit.scoreboardapi.scoreboard

import gt.creeperface.nukkit.scoreboardapi.packet.SetScorePacket
import gt.creeperface.nukkit.scoreboardapi.packet.data.ScoreInfo
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap
import it.unimi.dsi.fastutil.longs.LongOpenHashSet

/**
 * @author CreeperFace
 */
class Objective(val name: String, val criteria: ObjectiveCriteria) {

    private val scores = Long2ObjectOpenHashMap<Score>()

    private val modified = LongOpenHashSet()
    private val renamed = LongOpenHashSet()

    private var cachedPacket: SetScorePacket? = null

    lateinit var displayName: String

    fun setScore(id: Long, name: String, score: Int) {
        val old = scores[id]

        scores[id] = Score(id, name, score)
        clearCache()

        old?.let {
            if (old.name != name) {
                renamed.add(id)
            }
        }

        modified.add(id)
    }

    fun getScore(id: Long): Score? = scores.get(id)

    fun resetScore(id: Long) {
        scores.remove(id)
        renamed.remove(id)

        modified.add(id)
    }

    fun getChanges(): List<SetScorePacket> {
        if(modified.isEmpty()) {
            return emptyList()
        }

        val setList = mutableListOf<ScoreInfo>()
        val removeList = mutableListOf<ScoreInfo>()

        modified.forEach { id ->
            if(scores.containsKey(id)) {
                val score = getScore(id)

                score?.let {
                    setList.add(ScoreInfo(id, this.name, score.value, score.name))
                }
            } else {
                removeList.add(ScoreInfo(id, this.name, 0, ""))
            }
        }

        renamed.forEach { id ->
            if (scores.containsKey(id)) {
                val score = getScore(id)

                score?.let {
                    removeList.add(ScoreInfo(id, this.name, score.value, score.name))
                }
            }
        }

        val packets = mutableListOf<SetScorePacket>()

        if (removeList.isNotEmpty()) {
            packets.add(SetScorePacket(SetScorePacket.Action.REMOVE, removeList))
        }

        if(setList.isNotEmpty()) {
            packets.add(SetScorePacket(SetScorePacket.Action.SET, setList))
        }

        return packets
    }

    fun getScorePacket(): SetScorePacket? {
        cachedPacket?.let {
            return it
        }

        if(scores.isEmpty()) {
            return null
        }

        val infos = mutableListOf<ScoreInfo>()

        scores.forEach { id, score ->
            infos.add(ScoreInfo(id, this.name, score.value, score.name))
        }

        val pk = SetScorePacket(SetScorePacket.Action.SET, infos)
        pk.encode()
        pk.isEncoded = true

        this.cachedPacket = pk
        return pk
    }

    fun resetChanges() {
        modified.clear()
    }

    fun clearCache() {
        cachedPacket = null
    }
}