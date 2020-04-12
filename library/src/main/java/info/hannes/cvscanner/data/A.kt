package info.hannes.cvscanner.data

sealed class A {

    class B : A() {
        class E : A() //this works.
    }

    class C : A()

    init {
        println("sealed class A")
    }

}