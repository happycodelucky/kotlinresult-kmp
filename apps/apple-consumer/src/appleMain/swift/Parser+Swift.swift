//
// Consumer Swift built on KotlinResult's bundled helpers — the way a downstream
// library wraps its own Kotlin API for Swift callers. Compiling this inside
// AppleConsumerKit proves `get()`, `get(as:)`, `result(as:)` and
// `KotlinThrowable: Error` are visible to a consumer that exports :kotlinresult.
//

import Foundation

extension Parser {
    /// The parsed number, or the Kotlin exception thrown as a Swift `Error`.
    public func int(_ text: String) throws -> Int {
        try parseInt(text: text).get()
    }

    /// The parse as a `Swift.Result`.
    public func intResult(_ text: String) -> Swift.Result<Int, any Error> {
        parseInt(text: text).result(as: Int.self)
    }

    /// Throws unless `ok` — `get()` on a `KotlinResult<KotlinUnit>`.
    public func require(_ ok: Bool) throws {
        try check(ok: ok).get()
    }
}
