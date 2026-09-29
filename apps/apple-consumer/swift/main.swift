// Runtime check of KotlinResult's Swift helpers through a consumer framework
// (`mise run test:swift`). Exits non-zero on the first mismatch.

import AppleConsumerKit
import Foundation

func expect(_ condition: Bool, _ message: String) {
    if !condition {
        FileHandle.standardError.write(Data("FAIL: \(message)\n".utf8))
        exit(1)
    }
}

let parser = Parser.shared

// get() on success, and on failure throwing the Kotlin exception itself.
expect((try? parser.int("42")) == 42, "get() returns the success value")
do {
    _ = try parser.int("x")
    expect(false, "get() throws on failure")
} catch let error as KotlinThrowable {
    expect(error.message == "not a number: x", "get() throws the Kotlin exception verbatim")
}

// result(as:) bridges to Swift.Result.
if case .success(let n) = parser.intResult("7") { expect(n == 7, "result(as:) success") } else { expect(false, "result(as:) success") }
if case .failure = parser.intResult("?") {} else { expect(false, "result(as:) failure") }

// get() on KotlinResult<KotlinUnit>.
expect((try? parser.require(true)) != nil, "get() on Unit success returns")
expect((try? parser.require(false)) == nil, "get() on Unit failure throws")

// Constructed from Swift, with a wrong type caught as KotlinResultTypeMismatchError.
let direct = KotlinResult<NSString>(value: "hello")
expect((try? direct.get(as: String.self)) == "hello", "init(value:) + get(as:)")
do {
    _ = try direct.get(as: Int.self)
    expect(false, "type mismatch throws")
} catch is KotlinResultTypeMismatchError {}

print("KotlinResult Swift helpers: all checks passed")
