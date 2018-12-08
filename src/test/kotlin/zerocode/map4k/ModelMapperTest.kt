package zerocode.map4k

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.bson.types.ObjectId
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import com.github.pseudometa.map4k.ModelMapper
import com.github.pseudometa.map4k.conversions.ArrayListToListConverter
import com.github.pseudometa.map4k.conversions.EnumToStringConverter
import com.github.pseudometa.map4k.conversions.ListToArrayListConverter
import com.github.pseudometa.map4k.conversions.ObjectIdToStringConverter
import com.github.pseudometa.map4k.conversions.StringToEnumConverter
import com.github.pseudometa.map4k.conversions.StringToObjectIdConverter
import com.github.pseudometa.map4k.conversions.TypeMap
import com.github.pseudometa.map4k.conversions.TypeMapRegistry
import com.github.pseudometa.map4k.conversions.TypeMappingException
import com.github.pseudometa.map4k.conversions.ValueConverterRegistry
import kotlin.reflect.KClass

fun aRandomId(): String =
    ObjectId.get().toHexString()

class ModelMapperTest {

    interface SourceType {
        val id: String
    }

    interface TargetType1 {
        val id: ObjectId
    }

    interface TargetType2 {
        val id: String
    }

    interface UnknownType

    data class SourceTypeImpl1(override val id: String = aRandomId()) : SourceType
    data class SourceTypeImpl2(override val id: String = aRandomId(), val name: String = "don pablo") : SourceType
    data class SourceTypeImpl3(override val id: String = aRandomId(), val amount: Double = 99.9) : SourceType
    data class TargetType1Impl1(override val id: ObjectId, val name: String) : TargetType1
    data class TargetType1Impl2(override val id: ObjectId, val amount: Double) : TargetType1
    data class TargetType2Impl(override val id: String) : TargetType2

    @Suppress("UNCHECKED_CAST")
    class TargetType1Map : TypeMap<TargetType1> {
        override fun getMappedType(source: Any): KClass<TargetType1> =
            when (source) {
                is SourceTypeImpl2 -> TargetType1Impl1::class
                is SourceTypeImpl3 -> TargetType1Impl2::class
                else -> throw IllegalArgumentException("Mapping not defined for ${source::class.simpleName}.")
            } as KClass<TargetType1>
    }

    @Suppress("UNCHECKED_CAST")
    class TargetType2Map : TypeMap<TargetType2> {
        override fun getMappedType(source: Any): KClass<TargetType2> =
            when (source) {
                is SourceTypeImpl1 -> TargetType2Impl::class
                else -> throw IllegalArgumentException("Mapping not defined for ${source::class.simpleName}.")
            } as KClass<TargetType2>
    }

    private val mapper = ModelMapper(
        typeMapRegistry = TypeMapRegistry(TargetType1Map(), TargetType2Map()),
        valueConverterRegistry = ValueConverterRegistry(
            ObjectIdToStringConverter(),
            StringToObjectIdConverter(),
            ListToArrayListConverter(),
            ArrayListToListConverter(),
            EnumToStringConverter(),
            StringToEnumConverter()
        )
    )

    @Test
    fun `can map from string to string`() {
        val target = mapper.mapTo<String>("some_value")
        assertThat(target, equalTo("some_value"))
    }

    @Test
    fun `can map from int to int`() {
        val target = mapper.mapTo<Int>(10)
        assertThat(target, equalTo(10))
    }

    @Test
    fun `can map from list to list`() {
        val target = mapper.mapTo<List<Int>>(listOf(1, 2, 3))
        assertThat(target, equalTo(listOf(1, 2, 3)))
    }

    @Test
    fun `can map from data class to itself`() {
        data class Source(val id: String = "some_id", val amount: Double = 105.7)

        val target = mapper.mapTo<Source>(Source())
        assertThat(target, equalTo(Source()))
    }

    @Test
    fun `can map from simple data class to simple data class`() {
        data class Source(val id: String = "some_id", val amount: Double = 105.7)
        data class Target(val id: String, val amount: Double)

        val source = Source()
        val target = mapper.mapTo<Target>(Source())
        assertThat(target, equalTo(Target(source.id, source.amount)))
    }

    @Test
    fun `throws where source and target have no matching properties`() {
        data class Source(val id: String = "")
        data class Target(val name: String)
        assertThrows<TypeMappingException> {
            mapper.mapTo<Target>(Source())
        }
    }

    @Test
    fun `throws where source and target have matching properties names but incompatible types and no converter`() {
        data class Source(val id: String = "")
        data class Target(val id: Double)
        assertThrows<TypeMappingException> {
            mapper.mapTo<Target>(Source())
        }
    }

    @Test
    fun `can map from simple data class to simple data class with an optional property`() {
        data class Source(val id: String = "some_id")
        data class Target(val id: String, val amount: Double = 105.7)

        val source = Source()
        val target = mapper.mapTo<Target>(Source())
        assertThat(target, equalTo(Target(source.id, 105.7)))
    }

    @Test
    fun `can map from source to target with a value conversion from string to objectId`() {
        data class Source(val id: String = aRandomId())
        data class Target(val id: ObjectId)

        val source = Source()
        val target = mapper.mapTo<Target>(source)
        assertThat(target, equalTo(Target(id = ObjectId(source.id))))
    }

    @Test
    fun `can map from source to target with a value conversion from objectId to string`() {
        data class Source(val id: ObjectId = ObjectId.get())
        data class Target(val id: String)

        val source = Source()
        val target = mapper.mapTo<Target>(source)
        assertThat(target, equalTo(Target(id = source.id.toHexString())))
    }

    enum class Status { New }

    @Test
    fun `can map from source to target with an enum to string conversion`() {
        data class Source(val status: Status = Status.New)
        data class Target(val status: String)

        val target = mapper.mapTo<Target>(Source())
        assertThat(target, equalTo(Target(status = Status.New.toString())))
    }

    @Test
    fun `can map from source to target with a string to enum conversion`() {
        data class Source(val status: String = "New")
        data class Target(val status: Status)

        val target = mapper.mapTo<Target>(Source())
        assertThat(target, equalTo(Target(status = Status.New)))
    }

    @Test
    fun `can map from source to target with a nested data class`() {
        data class NestedSource(val id: String = aRandomId())
        data class NestedTarget(val id: ObjectId)
        data class Source(val nested: NestedSource = NestedSource())
        data class Target(val nested: NestedTarget)

        val source = Source()
        val target = mapper.mapTo<Target>(source)
        assertThat(target, equalTo(Target(nested = NestedTarget(ObjectId(source.nested.id)))))
    }

    @Test
    fun `can map from source to target with a list to array list conversion`() {
        data class Source(val items: List<String> = listOf("item1", "items2"))
        data class Target(val items: ArrayList<String>)

        val source = Source()
        val expected = Target(items = arrayListOf("item1", "items2"))
        val actual = mapper.mapTo<Target>(source)
        assertThat(actual, equalTo(expected))
    }

    data class NestedSource(val id: String = aRandomId())
    data class NestedTarget(val id: ObjectId)
    data class Source(val items: List<NestedSource> = listOf(NestedSource(), NestedSource()))
    data class Target(val items: ArrayList<NestedTarget>)

    @Test
    fun `can map from source to target with a list of data class to array list conversion`() {
        val source = Source()
        val expected = Target(items = arrayListOf(
            NestedTarget(id = ObjectId(source.items[0].id)),
            NestedTarget(id = ObjectId(source.items[1].id)))
        )
        val actual = mapper.mapTo<Target>(source)
        assertThat(actual, equalTo(expected))
    }

    @Test
    fun `throws where mapping to an abstract type and no type model found`() {
        assertThrows<TypeMappingException> {
            data class Source(val id: String = "")
            mapper.mapTo<UnknownType>(Source())
        }
    }

    @Test
    fun `can map from data class to same abstract type`() {
        val source = SourceTypeImpl1()
        val target = mapper.mapTo<SourceType>(source) as SourceTypeImpl1
        assertThat(target, equalTo(SourceTypeImpl1(id = source.id)))
    }

    @Test
    fun `can map from data class to different abstract type`() {
        val source = SourceTypeImpl1()
        val target = mapper.mapTo<TargetType2>(source) as TargetType2Impl
        assertThat(target, equalTo(TargetType2Impl(id = source.id)))
    }

    @Test
    fun `can map from data class to data class with the same abstract property type`() {
        data class Source(val child: SourceType = SourceTypeImpl1())
        data class Target(val child: SourceType)

        val source = Source()
        val target = mapper.mapTo<Target>(source)
        assertThat(target, equalTo(Target(source.child)))
    }

    @Test
    fun `can map from data class to data class a target abstract property type`() {
        data class Source(val child: SourceTypeImpl1 = SourceTypeImpl1())
        data class Target(val child: SourceType)

        val source = Source()
        val target = mapper.mapTo<Target>(source)
        assertThat(target, equalTo(Target(source.child)))
    }

    @Test
    fun `can map from data class to data class with different abstract property types`() {
        data class Source(val child: SourceType)
        data class Target(val child: TargetType2)

        val sourceChild = SourceTypeImpl1()
        val source = Source(sourceChild)
        val target = mapper.mapTo<Target>(source)
        assertThat(target, equalTo(Target(child = TargetType2Impl(sourceChild.id))))
    }

    @Test
    fun `can map from source to target with a list of the same abstract type`() {
        data class Source(val child: List<SourceType> = listOf(SourceTypeImpl2(), SourceTypeImpl3()))
        data class Target(val child: List<SourceType>)

        val source = Source()
        val expected = Target(child = listOf(
            SourceTypeImpl2(id = source.child[0].id),
            SourceTypeImpl3(id = source.child[1].id)
        ))

        val actual = mapper.mapTo<Target>(source)
        assertThat(actual, equalTo(expected))
    }

    @Test
    fun `can map from source to target with a list of different abstract types`() {
        data class Source(val child: List<SourceType> = listOf(SourceTypeImpl2(), SourceTypeImpl3()))
        data class Target(val child: List<TargetType1>)

        val source = Source()
        val expected = Target(child = listOf(
            TargetType1Impl1(id = ObjectId(source.child[0].id), name = "don pablo"),
            TargetType1Impl2(id = ObjectId(source.child[1].id), amount = 99.9)
        ))
        val actual = mapper.mapTo<Target>(source)
        assertThat(actual, equalTo(expected))
    }

    @Test
    fun `can map from source to target with a list of different abstract types and a conversion from arraylist to list type`() {
        data class Source(val child: ArrayList<SourceType> = arrayListOf(SourceTypeImpl2(), SourceTypeImpl3()))
        data class Target(val child: List<TargetType1>)

        val source = Source()
        val expected = Target(child = listOf(
            TargetType1Impl1(id = ObjectId(source.child[0].id), name = "don pablo"),
            TargetType1Impl2(id = ObjectId(source.child[1].id), amount = 99.9)
        ))

        val actual = mapper.mapTo<Target>(source)
        assertThat(actual, equalTo(expected))
    }

    @Test
    fun `can map from source to target with a list of different abstract types and a conversion from list to arraylist type`() {
        data class Source(val child: List<SourceType> = listOf(SourceTypeImpl2(), SourceTypeImpl3()))
        data class Target(val child: ArrayList<TargetType1>)

        val source = Source()
        val expected = Target(child = arrayListOf(
            TargetType1Impl1(id = ObjectId(source.child[0].id), name = "don pablo"),
            TargetType1Impl2(id = ObjectId(source.child[1].id), amount = 99.9)
        ))

        val actual = mapper.mapTo<Target>(source)
        assertThat(actual, equalTo(expected))
    }

    @Test
    fun `can map from source to target with nested lists of different abstract types and a conversion from list to arraylist type`() {
        data class Source(val child: List<List<SourceType>> = listOf(listOf(SourceTypeImpl2(), SourceTypeImpl3())))
        data class Target(val child: ArrayList<ArrayList<TargetType1>>)

        val source = Source()
        val expected = Target(child = arrayListOf(
            arrayListOf(
                TargetType1Impl1(id = ObjectId(source.child[0][0].id), name = "don pablo"),
                TargetType1Impl2(id = ObjectId(source.child[0][1].id), amount = 99.9)
            )))
        val actual = mapper.mapTo<Target>(source)
        assertThat(actual, equalTo(expected))
    }

    @Test
    fun `can map from source to target with nested lists of nested lists of different abstract types and a conversion from arraylist to list type`() {
        data class Source(val child: ArrayList<ArrayList<SourceType>> = arrayListOf(arrayListOf(SourceTypeImpl2(), SourceTypeImpl3())))
        data class Target(val child: List<List<TargetType1>>)

        val source = Source()
        val expected = Target(child = listOf(
            listOf(
                TargetType1Impl1(id = ObjectId(source.child[0][0].id), name = "don pablo"),
                TargetType1Impl2(id = ObjectId(source.child[0][1].id), amount = 99.9)
            )))
        val actual = mapper.mapTo<Target>(source)
        assertThat(actual, equalTo(expected))
    }

    @Test
    fun `can map from source to target with nested lists of nested lists of nested lists`() {
        data class Source(val child: ArrayList<ArrayList<List<SourceType>>> = arrayListOf(arrayListOf(listOf(SourceTypeImpl2(), SourceTypeImpl3()))))
        data class Target(val child: List<List<ArrayList<TargetType1>>>)

        val source = Source()
        val expected = Target(child = listOf(
            listOf(
                arrayListOf(
                    TargetType1Impl1(id = ObjectId(source.child[0][0][0].id), name = "don pablo"),
                    TargetType1Impl2(id = ObjectId(source.child[0][0][1].id), amount = 99.9)
                )))
        )
        val actual = mapper.mapTo<Target>(source)
        assertThat(actual, equalTo(expected))
    }
}