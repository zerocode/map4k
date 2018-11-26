package zercode.map4k.extensions

inline fun <T : Any, U : Any> List<T>.joinWhere(other: List<U>, where: (T, U) -> Boolean): List<Pair<T, U>> =
    mapNotNull { t -> other.firstOrNull { u -> where(t, u) }?.let { u -> Pair(t, u) } }