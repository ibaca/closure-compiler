/*
 * Copyright 2014 The Closure Compiler Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.javascript.jscomp;

import com.google.javascript.jscomp.CompilerOptions.LanguageMode;

/**
 * Test case for {@link Es6RewriteBlockScopedDeclaration}.
 *
 * @author moz@google.com (Michael Zhou)
 */
public final class Es6RewriteBlockScopedDeclarationTest extends TypeICompilerTestCase {

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    setAcceptedLanguage(LanguageMode.ECMASCRIPT_2015);
    enableRunTypeCheckAfterProcessing();
    this.mode = TypeInferenceMode.NEITHER;
  }

  @Override
  protected CompilerOptions getOptions() {
    CompilerOptions options = super.getOptions();
    options.setLanguageOut(LanguageMode.ECMASCRIPT3);
    return options;
  }

  @Override
  protected CompilerPass getProcessor(Compiler compiler) {
    return new Es6RewriteBlockScopedDeclaration(compiler);
  }

  @Override
  protected int getNumRepetitions() {
    return 1;
  }

  public void testSimple() {
    test("let x = 3;", "var x = 3;");
    test("const x = 3;", "/** @const */ var x = 3;");
    test("const x = 1, y = 2;", "/** @const */ var x = 1; /** @const */ var y = 2;");
    test("const a = 0; a;", "/** @const */ var a = 0; a;");
    test("if (a) { let x; }", "if (a) { var x; }");
    test("function f() { const x = 3; }",
        "function f() { /** @const */ var x = 3; }");
  }

  public void testLetShadowing() {
    test(
        lines(
            "function f() {",
            "  var x = 1;",
            "  if (a) {",
            "    let x = 2;",
            "    x = function() { return x; };",
            "  }",
            "  return x;",
            "}"),
        lines(
            "function f() {",
            "  var x = 1;",
            "  if (a) {",
            "    var x$0 = 2;",
            "    x$0 = function() { return x$0; };",
            "  }",
            "  return x;",
            "}"));

    test(
        lines(
            "function f() {",
            "  const x = 3;",
            "  if (true) {",
            "    let x;",
            "  }",
            "}"),
        lines(
            "function f() {",
            "  /** @const */ var x = 3;",
            "  if (true) {",
            "    var x$0;",
            "  }",
            "}"));

    test(
        lines(
            "function f() {",
            "  var x = 1;",
            "  if (a) {",
            "    var g = function() { return x; };",
            "    let x = 2;",
            "    return g();",
            "  }",
            "}"),
        lines(
            "function f() {",
            "  var x = 1;",
            "  if (a) {",
            "    var g = function() { return x$0; };",
            "    var x$0 = 2;",
            "    return g();",
            "  }",
            "}"));

    test(
        lines(
            "var x = 2;",
            "function f() {",
            "  x = 1;",
            "  if (a) {",
            "    let x = 2;",
            "  }",
            "}"),
        lines(
            "var x = 2;",
            "function f() {",
            "  x = 1;",
            "  if (a) {",
            "    var x$0 = 2;",
            "  }",
            "}"));

    test(
        lines(
            "function f() {",
            "  {",
            "    let inner = 2;",
            "  }",
            "  use(inner)",
            "}"),
        lines(
            "function f() {",
            "  {",
            "    var inner$0 = 2;",
            "  }",
            "  use(inner)",
            "}"));
  }

  public void testNonUniqueLet() {
    test(
        lines(
            "function f() {",
            "  var x = 1;",
            "  if (a) {",
            "    let x = 2;",
            "    assert(x === 2);",
            "  }",
            "  if (b) {",
            "    let x;",
            "    assert(x === undefined);",
            "  }",
            "  assert(x === 1);",
            "}"),
        lines(
            "function f() {",
            "  var x = 1;",
            "  if (a) {",
            "    var x$0 = 2;",
            "    assert(x$0 === 2);",
            "  }",
            "  if (b) {",
            "    var x$1;",
            "    assert(x$1 === undefined);",
            "  }",
            "  assert(x === 1);",
            "}"));

    test(
        lines(
            "function f() {",
            "  if (a) {",
            "    let x = 2;",
            "    assert(x === 2);",
            "    if (b) {",
            "      let x;",
            "      assert(x === undefined);",
            "    }",
            "  }",
            "}"),
        lines(
            "function f() {",
            "  if (a) {",
            "    var x = 2;",
            "    assert(x === 2);",
            "    if (b) {",
            "      var x$0;",
            "      assert(x$0 === undefined);",
            "    }",
            "  }",
            "}"));
  }

  public void testRenameConflict() {
    test(
        lines(
            "function f() {",
            "  let x = 1;",
            "  let x$0 = 2;",
            "  {",
            "    let x = 3;",
            "  }",
            "}"),
        lines(
            "function f() {",
            "  var x = 1;",
            "  var x$0 = 2;",
            "  {",
            "    var x$1 = 3;",
            "  }",
            "}"));
  }

  public void testForOfLoop() {
    test(
        lines(
            "function f() {",
            "  let x = 5;",
            "  for (let x of [1,2,3]) {",
            "    console.log(x);",
            "  }",
            "  console.log(x);",
            "}"),
        lines(
            "function f() {",
            "  var x = 5;",
            "  for(var x$0 of [1,2,3]) {",
            "    console.log(x$0);",
            "  }",
            "  console.log(x);",
            "}"));

    test(
        lines(
            "function f() {",
            "  let x = 5;",
            "  for (let x of [1,2,3]) {",
            "    let x = 123;",
            "    console.log(x);",
            "  }",
            "  console.log(x);",
            "}"),
        lines(
            "function f() {",
            "  var x = 5;",
            "  for(var x$0 of [1,2,3]) {",
            "    var x$1 = 123;",
            "    console.log(x$1);",
            "  }",
            "  console.log(x);",
            "}"));
  }

  public void testForLoop() {
    test(
        lines(
            "function f() {",
            "  const y = 0;",
            "  for (let x = 0; x < 10; x++) {",
            "    const y = x * 2;",
            "    const z = y;",
            "  }",
            "  console.log(y);",
            "}"),
        lines(
            "function f() {",
            "  /** @const */ var y = 0;",
            "  for (var x = 0; x < 10; x++) {",
            "    /** @const */ var y$0 = x * 2;",
            "    /** @const */ var z = y$0;",
            "  }",
            "  console.log(y);",
            "}"));

    test(
        lines(
            "for (let i in [0, 1]) {",
            "  function f() {",
            "    let i = 0;",
            "    if (true) {",
            "      let i = 1;",
            "    }",
            "  }",
            "}"),
        lines(
            "for (var i in [0, 1]) {",
            "  var f = function() {",
            "    var i = 0;",
            "    if (true) {",
            "      var i$0 = 1;",
            "    }",
            "  }",
            "}"));

    test(
        "for (let i = 0;;) { let i; }",
        "for (var i = 0;;) { var i$0 = undefined; }");

    test(
        "for (let i = 0;;) {} let i;",
        "for (var i$0 = 0;;) {} var i;");

    test(
        lines(
            "for (var x in y) {",
            "  /** @type {number} */",
            "  let i;",
            "}"),
        lines(
            "for (var x in y) {",
            "  /** @type {number} */",
            "  var i = /** @type {?} */ (undefined);",
            "}"));

    test(lines(
            "for (const i in [0, 1]) {",
            "  function f() {",
            "    let i = 0;",
            "    if (true) {",
            "      let i = 1;",
            "    }",
            "  }",
            "}"),
        lines(
            "for (var i in [0, 1]) {",
            "  var f = function() {",
            "    var i = 0;",
            "    if (true) {",
            "      var i$0 = 1;",
            "    }",
            "  }",
            "}"));
  }

  public void testFunctionInLoop() {
    test(
        lines(
            "for (var x of y) {",
            "  function f() {",
            "    let z;",
            "  }",
            "}"),
        lines(
            "for (var x of y) {",
            "  var f = function() {",
            "    var z;",
            "  };",
            "}"));
  }

  public void testLoopClosure() {
    test(
        lines(
            "const arr = [];",
            "for (let i = 0; i < 10; i++) {",
            "  arr.push(function() { return i; });",
            "}"),
        lines(
            "/** @const */ var arr = [];",
            "var $jscomp$loop$0 = {};",
            "$jscomp$loop$0.i = 0;",
            "for (; $jscomp$loop$0.i < 10;",
            "    $jscomp$loop$0 = {i: $jscomp$loop$0.i}, $jscomp$loop$0.i++) {",
            "  arr.push((function($jscomp$loop$0) {",
            "      return function() { return $jscomp$loop$0.i; };",
            "  })($jscomp$loop$0));",
            "}"));

    test(
        lines(
            "const arr = [];",
            "for (let i = 0; i < 10; i++) {",
            "  let y = i;",
            "  arr.push(function() { return y; });",
            "}"),
        lines(
            "/** @const */ var arr = [];",
            "var $jscomp$loop$0 = {};",
            "var i = 0;",
            "for (; i < 10; $jscomp$loop$0 = {y: $jscomp$loop$0.y}, i++) {",
            "  $jscomp$loop$0.y = i;",
            "  arr.push((function($jscomp$loop$0) {",
            "      return function() { return $jscomp$loop$0.y; };",
            "  })($jscomp$loop$0));",
            "}"));

    test(
        lines(
            "const arr = [];",
            "while (true) {",
            "  let i = 0;",
            "  arr.push(function() { return i; });",
            "}"),
        lines(
            "/** @const */ var arr = [];",
            "var $jscomp$loop$0 = {}",
            "while (true) {",
            "  $jscomp$loop$0.i = 0;",
            "  arr.push((function($jscomp$loop$0) {",
            "      return function() { return $jscomp$loop$0.i; };",
            "  })($jscomp$loop$0));",
            "  $jscomp$loop$0 = {i: $jscomp$loop$0.i}",
            "}"));

    test(
        lines(
            "const arr = [];",
            "for (let i = 0; i < 10; i++) {",
            "  let y = i;",
            "  arr.push(function() { return y + i; });",
            "}"),
        lines(
            "/** @const */ var arr = [];",
            "var $jscomp$loop$0 = {};",
            "$jscomp$loop$0.i = 0;",
            "for (; $jscomp$loop$0.i < 10;",
            "    $jscomp$loop$0 = {y: $jscomp$loop$0.y, i: $jscomp$loop$0.i},",
            "        $jscomp$loop$0.i++) {",
            "  $jscomp$loop$0.y = $jscomp$loop$0.i;",
            "  arr.push((function($jscomp$loop$0) {",
            "          return function() {",
            "              return $jscomp$loop$0.y + $jscomp$loop$0.i;",
            "          };",
            "  }($jscomp$loop$0)));",
            "}"));

    // Renamed inner i
    test(
        lines(
            "const arr = [];",
            "let x = 0",
            "for (let i = 0; i < 10; i++) {",
            "  let i = x + 1;",
            "  arr.push(function() { return i + i; });",
            "  x++;",
            "}"),
        lines(
            "/** @const */ var arr = [];",
            "var x = 0",
            "var $jscomp$loop$1 = {};",
            "var i = 0;",
            "for (; i < 10; $jscomp$loop$1 = {i$0: $jscomp$loop$1.i$0}, i++) {",
            "  $jscomp$loop$1.i$0 = x + 1;",
            "  arr.push((function($jscomp$loop$1) {",
            "      return function() {",
            "          return $jscomp$loop$1.i$0 + $jscomp$loop$1.i$0;",
            "      };",
            "  }($jscomp$loop$1)));",
            "  x++;",
            "}"));

    // Renamed, but both closures reference the inner i
    test(
        lines(
            "const arr = [];",
            "let x = 0",
            "for (let i = 0; i < 10; i++) {",
            "  arr.push(function() { return i + i; });",
            "  let i = x + 1;",
            "  arr.push(function() { return i + i; });",
            "  x++;",
            "}"),
        lines(
            "/** @const */ var arr = [];",
            "var x = 0",
            "var $jscomp$loop$1 = {};",
            "var i = 0;",
            "for (; i < 10; $jscomp$loop$1 = {i$0: $jscomp$loop$1.i$0}, i++) {",
            "  arr.push((function($jscomp$loop$1) {",
            "      return function() {",
            "          return $jscomp$loop$1.i$0 + $jscomp$loop$1.i$0;",
            "      };",
            "  }($jscomp$loop$1)));",
            "  $jscomp$loop$1.i$0 = x + 1;",
            "  arr.push((function($jscomp$loop$1) {",
            "      return function() {",
            "          return $jscomp$loop$1.i$0 + $jscomp$loop$1.i$0;",
            "      };",
            "  }($jscomp$loop$1)));",
            "  x++;",
            "}"));

    // Renamed distinct captured variables
    test(
        lines(
            "for (let i = 0; i < 10; i++) {",
            "  if (true) {",
            "    let i = x - 1;",
            "    arr.push(function() { return i + i; });",
            "  }",
            "  let i = x + 1;",
            "  arr.push(function() { return i + i; });",
            "  x++;",
            "}"),
        lines(
            "var $jscomp$loop$2 = {};",
            "var i = 0;",
            "for (; i < 10;",
            "    $jscomp$loop$2 = {i$0: $jscomp$loop$2.i$0, i$1: $jscomp$loop$2.i$1}, i++) {",
            "  if (true) {",
            "    $jscomp$loop$2.i$0 = x - 1;",
            "    arr.push((function($jscomp$loop$2) {",
            "        return function() { return $jscomp$loop$2.i$0 + $jscomp$loop$2.i$0; };",
            "    })($jscomp$loop$2));",
            "  }",
            "  $jscomp$loop$2.i$1 = x + 1;",
            "  arr.push((function($jscomp$loop$2) {",
            "      return function() { return $jscomp$loop$2.i$1 + $jscomp$loop$2.i$1; };",
            "  })($jscomp$loop$2));",
            "  x++;",
            "}"));

    // Preserve type annotation
    test(
        "for (;;) { /** @type {number} */ let x = 3; var f = function() { return x; } }",
        lines(
            "var $jscomp$loop$0 = {};",
            "for (;;$jscomp$loop$0 = {x: $jscomp$loop$0.x}) {",
            "  /** @type {number} */ $jscomp$loop$0.x = 3;",
            "  var f = function($jscomp$loop$0) {",
            "    return function() { return $jscomp$loop$0.x}",
            "  }($jscomp$loop$0);",
            "}"));

    // Preserve inline type annotation
    test(
        "for (;;) { let /** number */ x = 3; var f = function() { return x; } }",
        lines(
            "var $jscomp$loop$0 = {};",
            "for (;;$jscomp$loop$0 = {x: $jscomp$loop$0.x}) {",
            "  /** @type {number} */ $jscomp$loop$0.x = 3;",
            "  var f = function($jscomp$loop$0) {",
            "    return function() { return $jscomp$loop$0.x}",
            "  }($jscomp$loop$0);",
            "}"));

    // Preserve inline type annotation and constancy
    test(
        "for (;;) { const /** number */ x = 3; var f = function() { return x; } }",
        lines(
            "var $jscomp$loop$0 = {};",
            "for (;;$jscomp$loop$0 = {x: $jscomp$loop$0.x}) {",
            "  /** @const @type {number} */ $jscomp$loop$0.x = 3;",
            "  var f = function($jscomp$loop$0) {",
            "    return function() { return $jscomp$loop$0.x}",
            "  }($jscomp$loop$0);",
            "}"));

    // Preserve inline type annotation on declaration lists
    test(lines(
        "for (;;) { let /** number */ x = 3, /** number */ y = 4;",
        "var f = function() { return x + y; } }"),
        lines(
            "var $jscomp$loop$0 = {};",
            "for (;;$jscomp$loop$0 = {x: $jscomp$loop$0.x, y: $jscomp$loop$0.y}) {",
            "  /** @type {number} */ $jscomp$loop$0.x = 3;",
            "  /** @type {number} */ $jscomp$loop$0.y = 4;",
            "  var f = function($jscomp$loop$0) {",
            "    return function() { return $jscomp$loop$0.x + $jscomp$loop$0.y}",
            "  }($jscomp$loop$0);",
            "}"));

    // Preserve inline type annotation and constancy on declaration lists
    test(lines(
        "for (;;) { const /** number */ x = 3, /** number */ y = 4;",
        "var f = function() { return x + y; } }"),
        lines(
            "var $jscomp$loop$0 = {};",
            "for (;;$jscomp$loop$0 = {x: $jscomp$loop$0.x, y: $jscomp$loop$0.y}) {",
            "  /** @const @type {number} */ $jscomp$loop$0.x = 3;",
            "  /** @const @type {number} */ $jscomp$loop$0.y = 4;",
            "  var f = function($jscomp$loop$0) {",
            "    return function() { return $jscomp$loop$0.x + $jscomp$loop$0.y}",
            "  }($jscomp$loop$0);",
            "}"));

    // No-op, vars don't need transpilation
    testSame("for (;;) { var /** number */ x = 3; var f = function() { return x; } }");

    test(
        lines(
            "var i;",
            "for (i = 0;;) {",
            "  let x = 0;",
            "  var f = function() { x; };",
            "}"),
        lines(
            "var i;",
            "var $jscomp$loop$0={};",
            "i = 0;",
            "for(;;$jscomp$loop$0 = {x: $jscomp$loop$0.x}) {",
            "  $jscomp$loop$0.x = 0;",
            "  var f = (function($jscomp$loop$0) {",
            "    return function() { $jscomp$loop$0.x; };",
            "  })($jscomp$loop$0);",
            "}"));

    test(
        lines("for (foo();;) {",
            "  let x = 0;",
            "  var f = function() { x; };",
            "}"),
        lines(
            "var $jscomp$loop$0={};",
            "foo();",
            "for(;;$jscomp$loop$0 = {x: $jscomp$loop$0.x}) {",
            "  $jscomp$loop$0.x = 0;",
            "  var f = (function($jscomp$loop$0) {",
            "    return function() { $jscomp$loop$0.x; };",
            "  })($jscomp$loop$0);",
            "}"));

    test(
        lines(
            "for (function foo() {};;) {",
            "  let x = 0;",
            "  var f = function() { x; };",
            "}"),
        lines(
            "var $jscomp$loop$0={};",
            "(function foo() {});",
            "for(;;$jscomp$loop$0 = {x: $jscomp$loop$0.x}) {",
            "  $jscomp$loop$0.x = 0;",
            "  var f = (function($jscomp$loop$0) {",
            "    return function() { $jscomp$loop$0.x; };",
            "  })($jscomp$loop$0);",
            "}"));

    test(
        lines(
            "for (;;) {",
            "  let x;",
            "  foo(function() { return x; });",
            "  x = 5;",
            "}"),
        lines(
            "var $jscomp$loop$0 = {};",
            "for(;;$jscomp$loop$0 = {x: $jscomp$loop$0.x}) {",
            "  $jscomp$loop$0.x = undefined;",
            "  foo(function($jscomp$loop$0) {",
            "    return function() {",
            "      return $jscomp$loop$0.x;",
            "    };",
            "  }($jscomp$loop$0));",
            "  $jscomp$loop$0.x=5;",
            "}"));
  }

  public void testLoopClosureCommaInBody() {
    test(
        lines(
            "const arr = [];",
            "let j = 0;",
            "for (let i = 0; i < 10; i++) {",
            "  let i, j = 0;",
            "  arr.push(function() { return i + j; });",
            "}"),
        lines(
            "/** @const */ var arr = [];",
            "var j = 0;",
            "var $jscomp$loop$1 = {};",
            "var i = 0;",
            "for (; i < 10; $jscomp$loop$1 = {i$0: $jscomp$loop$1.i$0,",
            "    j: $jscomp$loop$1.j}, i++) {",
            "    $jscomp$loop$1.i$0 = undefined;",
            "    $jscomp$loop$1.j = 0;",
            "  arr.push((function($jscomp$loop$1) {",
            "      return function() { return $jscomp$loop$1.i$0 + $jscomp$loop$1.j; };",
            "  })($jscomp$loop$1));",
            "}"));
  }

  public void testLoopClosureCommaInIncrement() {
    test(
        lines(
            "const arr = [];",
            "let j = 0;",
            "for (let i = 0; i < 10; i++, j++) {",
            "  arr.push(function() { return i + j; });",
            "}"),
        lines(
            "/** @const */ var arr = [];",
            "var j = 0;",
            "var $jscomp$loop$0 = {};",
            "$jscomp$loop$0.i = 0;",
            "for (; $jscomp$loop$0.i < 10;",
            "    $jscomp$loop$0 = {i: $jscomp$loop$0.i}, ($jscomp$loop$0.i++, j++)) {",
            "  arr.push((function($jscomp$loop$0) {",
            "      return function() { return $jscomp$loop$0.i + j; };",
            "  })($jscomp$loop$0));",
            "}"));
  }

  public void testLoopClosureCommaInInitializerAndIncrement() {
    test(
        lines(
            "const arr = [];",
            "for (let i = 0, j = 0; i < 10; i++, j++) {",
            "  arr.push(function() { return i + j; });",
            "}"),
        lines(
            "/** @const */ var arr = [];",
            "var $jscomp$loop$0 = {};",
            "$jscomp$loop$0.i = 0;",
            "$jscomp$loop$0.j = 0;",
            "for (; $jscomp$loop$0.i < 10;",
            "    $jscomp$loop$0 = {i: $jscomp$loop$0.i, j : $jscomp$loop$0.j},",
            "        ($jscomp$loop$0.i++, $jscomp$loop$0.j++)) {",
            "  arr.push((function($jscomp$loop$0) {",
            "      return function() { return $jscomp$loop$0.i + $jscomp$loop$0.j; };",
            "  })($jscomp$loop$0));",
            "}"));

    test(
        lines(
            "const arr = [];",
            "for (let i = 0, j = 0; i < 10; i++, j++) {",
            "  arr.push(function() { return j; });",
            "}"),
        lines(
            "/** @const */ var arr = [];",
            "var $jscomp$loop$0 = {};",
            "var i = 0;",
            "$jscomp$loop$0.j = 0;",
            "for (; i < 10; $jscomp$loop$0 = {j : $jscomp$loop$0.j},",
            "    (i++, $jscomp$loop$0.j++)) {",
            "  arr.push((function($jscomp$loop$0) {",
            "      return function() { return $jscomp$loop$0.j; };",
            "  })($jscomp$loop$0));",
            "}"));
  }

  public void testLoopClosureMutated() {
    test(
        lines(
            "const arr = [];",
            "for (let i = 0; i < 10; i++) {",
            "  arr.push(function() { return ++i; });",
            "}"),
        lines(
            "/** @const */ var arr = [];",
            "var $jscomp$loop$0 = {};",
            "$jscomp$loop$0.i = 0;",
            "for (; $jscomp$loop$0.i < 10;",
            "    $jscomp$loop$0 = {i: $jscomp$loop$0.i}, $jscomp$loop$0.i++) {",
            "  arr.push((function($jscomp$loop$0) {",
            "      return function() {",
            "          return ++$jscomp$loop$0.i;",
            "      };",
            "  }($jscomp$loop$0)));",
            "}"));

    test(
        lines(
            "const arr = [];",
            "for (let i = 0; i < 10; i++) {",
            "  arr.push(function() { return i; });",
            "  i += 100;",
            "}"),
        lines(
            "/** @const */ var arr = [];",
            "var $jscomp$loop$0 = {};",
            "$jscomp$loop$0.i = 0;",
            "for (; $jscomp$loop$0.i < 10;",
            "    $jscomp$loop$0 = {i: $jscomp$loop$0.i}, $jscomp$loop$0.i++) {",
            "  arr.push((function($jscomp$loop$0) {",
            "      return function() {",
            "          return $jscomp$loop$0.i;",
            "      };",
            "  }($jscomp$loop$0)));",
            "  $jscomp$loop$0.i += 100;",
            "}"));
  }

  public void testLoopClosureWithNestedInnerFunctions() {
    test(lines(
        "for (let i = 0; i < 10; i++) {",
        "  later(function(ctr) {",
        "    (function() { return use(i); })();",
        "  });",
        "}"),
        lines(
        "var $jscomp$loop$0 = {};",
        "$jscomp$loop$0.i = 0;",
        "for (; $jscomp$loop$0.i < 10;",
        "    $jscomp$loop$0 = {i: $jscomp$loop$0.i}, $jscomp$loop$0.i++) {",
        "  later((function($jscomp$loop$0) {",
        "    return function(ctr) {",
        "      (function() { return use($jscomp$loop$0.i); })();",
        "    };",
        "  })($jscomp$loop$0));",
        "}"));

    test(lines(
        "for (let i = 0; i < 10; i++) {",
        "  var f = function() {",
        "    return function() {",
        "      return i;",
        "    };",
        "  };",
        "}"),
        lines(
        "var $jscomp$loop$0 = {};",
        "$jscomp$loop$0.i = 0;",
        "for (; $jscomp$loop$0.i < 10;",
        "    $jscomp$loop$0 = {i: $jscomp$loop$0.i}, $jscomp$loop$0.i++) {",
        "  var f = function($jscomp$loop$0) {",
        "    return function() {",
        "      return function() {",
        "        return $jscomp$loop$0.i;",
        "      };",
        "    };",
        "  }($jscomp$loop$0);",
        "}"));

    test(lines(
        "use(function() {",
        "  later(function(ctr) {",
        "    for (let i = 0; i < 10; i++) {",
        "      (function() { return use(i); })();",
        "    }",
        "  });",
        "});"),
        lines(
        "use(function() {",
        "  later(function(ctr) {",
        "    var $jscomp$loop$0 = {};",
        "    $jscomp$loop$0.i = 0;",
        "    for (; $jscomp$loop$0.i < 10;",
        "        $jscomp$loop$0 = {i: $jscomp$loop$0.i}, $jscomp$loop$0.i++) {",
        "        (function($jscomp$loop$0) {",
        "          return function() { return use($jscomp$loop$0.i); }",
        "        })($jscomp$loop$0)();",
        "    }",
        "  });",
        "});"));
  }

  public void testNestedLoop() {
    test(
        lines(
            "function f() {",
            "  let arr = [];",
            "  for (let i = 0; i < 10; i++) {",
            "    for (let j = 0; j < 10; j++) {",
            "      arr.push(function() { return j++ + i++; });",
            "      arr.push(function() { return j++ + i++; });",
            "    }",
            "  }",
            "}"),
        lines(
            "function f() {",
            "  var arr = [];",
            "  var $jscomp$loop$1 = {};",
            "  $jscomp$loop$1.i = 0;",
            "  for (; $jscomp$loop$1.i < 10;",
            "      $jscomp$loop$1 = {i: $jscomp$loop$1.i}, $jscomp$loop$1.i++) {",
            "    var $jscomp$loop$0 = {};",
            "    $jscomp$loop$0.j = 0;",
            "    for (; $jscomp$loop$0.j < 10;",
            "        $jscomp$loop$0 = {j: $jscomp$loop$0.j}, $jscomp$loop$0.j++) {",
            "      arr.push((function($jscomp$loop$0, $jscomp$loop$1) {",
            "          return function() {",
            "              return $jscomp$loop$0.j++ + $jscomp$loop$1.i++;",
            "          };",
            "      }($jscomp$loop$0, $jscomp$loop$1)));",
            "      arr.push((function($jscomp$loop$0, $jscomp$loop$1) {",
            "          return function() {",
            "              return $jscomp$loop$0.j++ + $jscomp$loop$1.i++;",
            "          };",
            "      }($jscomp$loop$0, $jscomp$loop$1)));",
            "    }",
            "  }",
            "}"));

    // Renamed inner i
    test(
        lines(
            "function f() {",
            "  let arr = [];",
            "  for (let i = 0; i < 10; i++) {",
            "    arr.push(function() { return i++ + i++; });",
            "    for (let i = 0; i < 10; i++) {",
            "      arr.push(function() { return i++ + i++; });",
            "    }",
            "  }",
            "}"),
        lines(
            "function f() {",
            "  var arr = [];",
            "  var $jscomp$loop$1 = {};",
            "  $jscomp$loop$1.i = 0;",
            "  for (; $jscomp$loop$1.i < 10;",
            "      $jscomp$loop$1 = {i: $jscomp$loop$1.i}, $jscomp$loop$1.i++) {",
            "    arr.push((function($jscomp$loop$1) {",
            "        return function() {",
            "            return $jscomp$loop$1.i++ + $jscomp$loop$1.i++;",
            "        };",
            "    }($jscomp$loop$1)));",
            "    var $jscomp$loop$2 = {};",
            "    $jscomp$loop$2.i$0 = 0;",
            "    for (; $jscomp$loop$2.i$0 < 10;",
            "        $jscomp$loop$2 = {i$0: $jscomp$loop$2.i$0}, $jscomp$loop$2.i$0++) {",
            "      arr.push((function($jscomp$loop$2) {",
            "          return function() {",
            "              return $jscomp$loop$2.i$0++ + $jscomp$loop$2.i$0++;",
            "          };",
            "      }($jscomp$loop$2)));",
            "    }",
            "  }",
            "}"));
  }

  public void testLabeledLoop() {
    test(
        lines(
            "label1:",
            "label2:",
            "for (let x = 1;;) {",
            "  function f() {",
            "    return x;",
            "  }",
            "}"),
        lines(
            "var $jscomp$loop$0 = {};",
            "$jscomp$loop$0.x = 1;",
            "label1:",
            "label2:",
            "for (;; $jscomp$loop$0 = {x: $jscomp$loop$0.x}) {",
            "  var f = function($jscomp$loop$0) {",
            "    return function f() {",
            "      return $jscomp$loop$0.x;",
            "    }",
            "  }($jscomp$loop$0);",
            "}"));
  }

  public void testForInAndForOf() {
    test(
        lines(
            "const arr = [];",
            "for (let i in [0, 1]) {",
            "  arr.push(function() { return i; });",
            "}"),
        lines(
            "/** @const */ var arr = [];",
            "var $jscomp$loop$0 = {};",
            "for (var i in [0, 1]) {",
            "  $jscomp$loop$0.i = i;",
            "  arr.push((function($jscomp$loop$0) {",
            "      return function() { return $jscomp$loop$0.i; };",
            "  })($jscomp$loop$0));",
            "  $jscomp$loop$0 = {i: $jscomp$loop$0.i};",
            "}"));

    test(
        lines(
            "const arr = [];",
            "for (let i of [0, 1]) {",
            "  let i = 0;",
            "  arr.push(function() { return i; });",
            "}"),
        lines(
            "/** @const */ var arr = [];",
            "var $jscomp$loop$1 = {};",
            "for (var i of [0, 1]) {",
            "  $jscomp$loop$1.i$0 = 0;",
            "  arr.push((function($jscomp$loop$1) {",
            "      return function() { return $jscomp$loop$1.i$0; };",
            "  })($jscomp$loop$1));",
            "  $jscomp$loop$1 = {i$0: $jscomp$loop$1.i$0}",
            "}"));

    test(
        lines(
            "for (;;) {",
            "  let a = getArray();",
            "  f = function() {",
            "    for (var x in use(a)) {",
            "      f(a);",
            "      a.push(x);",
            "      return x;",
            "    }",
            "  }",
            "}"),
        lines(
            "var $jscomp$loop$0 = {};",
            "for (;; $jscomp$loop$0 = {a: $jscomp$loop$0.a}) {",
            "  $jscomp$loop$0.a = getArray();",
            "  f = (function($jscomp$loop$0) {",
            "    return function() {",
            "      for (var x in use($jscomp$loop$0.a)) {",
            "        f($jscomp$loop$0.a);",
            "        $jscomp$loop$0.a.push(x);",
            "        return x;",
            "      }",
            "    };",
            "  }($jscomp$loop$0));",
            "}"));
  }

  public void testDoWhileForOfCapturedLet() {
    test(
        lines(
            "const arr = [];",
            "do {",
            "  let special = 99;",
            "  for (let i of [0, 1, special, 3, 4, 5]) {",
            "    i = Number(i);",
            "    arr.push(function() { return i++; });",
            "    arr.push(function() { return i + special; });",
            "  }",
            "} while (false);"),
        lines(
            "/** @const */ var arr = [];",
            "var $jscomp$loop$1 = {};",
            "do {",
            "  $jscomp$loop$1.special = 99;",
            "  var $jscomp$loop$0 = {};",
            "  for (var i of [0, 1, $jscomp$loop$1.special, 3, 4, 5]) {",
            "    $jscomp$loop$0.i = i",
            "    $jscomp$loop$0.i = Number($jscomp$loop$0.i);",
            "    arr.push((function($jscomp$loop$0) {",
            "        return function() { return $jscomp$loop$0.i++; };",
            "    }($jscomp$loop$0)));",
            "    arr.push((function($jscomp$loop$0, $jscomp$loop$1) {",
            "        return function() { return $jscomp$loop$0.i + $jscomp$loop$1.special; };",
            "    }($jscomp$loop$0, $jscomp$loop$1)));",
            "    $jscomp$loop$0 = {i: $jscomp$loop$0.i};",
            "  }",
            "  $jscomp$loop$1 = {special: $jscomp$loop$1.special};",
            "} while (false);"));
  }

  // https://github.com/google/closure-compiler/issues/1124
  public void testFunctionsInLoop() {
    test(
        lines(
            "while (true) {",
            "  let x = null;",
            "  var f = function() {",
            "    x();",
            "  }",
            "}"),
        lines(
            "var $jscomp$loop$0 = {};",
            "while (true) {",
            "  $jscomp$loop$0.x = null;",
            "  var f = function($jscomp$loop$0) {",
            "    return function() {",
            "      ($jscomp$loop$0.x)();",
            "    };",
            "  }($jscomp$loop$0);",
            "  $jscomp$loop$0 = {x:$jscomp$loop$0.x};",
            "}"));

    test(
        lines(
            "while (true) {",
            "  let x = null;",
            "  function f() {",
            "    x();",
            "  }",
            "}"),
        lines(
            "var $jscomp$loop$0 = {};",
            "while (true) {",
            "  $jscomp$loop$0.x = null;",
            "  var f = function($jscomp$loop$0) {",
            "    return function f() {",
            "      ($jscomp$loop$0.x)();",
            "    };",
            "  }($jscomp$loop$0);",
            "  $jscomp$loop$0 = {x:$jscomp$loop$0.x};",
            "}"));

    test(
        lines(
            "while (true) {",
            "  let x = null;",
            "  (function() {",
            "    x();",
            "  })();",
            "}"),
        lines(
            "var $jscomp$loop$0 = {};",
            "while (true) {",
            "  $jscomp$loop$0.x = null;",
            "  (function($jscomp$loop$0) {",
            "    return function () {",
            "      ($jscomp$loop$0.x)();",
            "    };",
            "  })($jscomp$loop$0)();",
            "  $jscomp$loop$0 = {x:$jscomp$loop$0.x};",
            "}"));
  }

  // https://github.com/google/closure-compiler/issues/1557
  public void testNormalizeDeclarations() {
    test(lines(
        "while(true) {",
        "  let x, y;",
        "  function f() {",
        "    x = 1;",
        "    y = 2;",
        "  }",
        "}"),
        lines(
        "var $jscomp$loop$0 = {};",
        "while(true) {",
        "  $jscomp$loop$0.x = undefined;",
        "  var f = function($jscomp$loop$0) {",
        "    return function f() {",
        "      $jscomp$loop$0.x = 1;",
        "      $jscomp$loop$0.y = 2;",
        "    }",
        "  }($jscomp$loop$0);",
        "  $jscomp$loop$0 = {x: $jscomp$loop$0.x, y: $jscomp$loop$0.y};",
        "}"));

    test(lines(
        "while(true) {",
        "  let x, y;",
        "  function f() {",
        "    y = 2;",
        "    x = 1;",
        "  }",
        "}"),
        lines(
        "var $jscomp$loop$0 = {};",
        "while(true) {",
        "  $jscomp$loop$0.x = undefined;",
        "  var f = function($jscomp$loop$0) {",
        "    return function f() {",
        "      $jscomp$loop$0.y = 2;",
        "      $jscomp$loop$0.x = 1;",
        "    }",
        "  }($jscomp$loop$0);",
        "  $jscomp$loop$0 = {y: $jscomp$loop$0.y, x: $jscomp$loop$0.x};",
        "}"));
  }

  public void testTypeAnnotationsOnLetConst() {
    this.mode = TypeInferenceMode.BOTH;

    Diagnostic mismatch =
        warningOtiNti(TypeValidator.TYPE_MISMATCH_WARNING, NewTypeInference.MISTYPED_ASSIGN_RHS);

    test(srcs("/** @type {number} */ let x = 5; x = 'str';"), mismatch);
    test(srcs("let /** number */ x = 5; x = 'str';"), mismatch);
    test(srcs("let /** @type {number} */ x = 5; x = 'str';"), mismatch);

    test(srcs("/** @type {number} */ const x = 'str';"), mismatch);
    test(srcs("const /** number */ x = 'str';"), mismatch);
    test(srcs("const /** @type {number} */ x = 'str';"), mismatch);
    test(srcs("const /** @type {string} */ x = 3, /** @type {number} */ y = 3;"), mismatch);
    test(srcs("const /** @type {string} */ x = 'str', /** @type {string} */ y = 3;"), mismatch);
  }

  public void testDoWhileForOfCapturedLetAnnotated() {
    this.mode = TypeInferenceMode.BOTH;

    test(
        lines(
            "while (true) {",
            "  /** @type {number} */ let x = 5;",
            "  (function() { x++; })();",
            "  x = 7;",
            "}"),
        null);

    test(
        lines(
            "for (/** @type {number} */ let x = 5;;) {",
            "  (function() { x++; })();",
            "  x = 7;",
            "}"),
        null);

    // TODO(sdh): NTI does not detect the type mismatch in the transpiled code,
    // since the $jscomp$loop$0 object does not have its type inferred until after
    // the mistyped assignment.
    test(
        srcs(
            lines(
                "while (true) {",
                "  /** @type {number} */ let x = 5;",
                "  (function() { x++; })();",
                "  x = 'str';",
                "}")),
        warningOtiNti(TypeValidator.TYPE_MISMATCH_WARNING, null));

    test(
        srcs(
            lines(
                "for (/** @type {number} */ let x = 5;;) {",
                "  (function() { x++; })();",
                "  x = 'str';",
                "}")),
        warningOtiNti(TypeValidator.TYPE_MISMATCH_WARNING, null));
  }

  public void testLetForInitializers() {
    test(
        lines(
            "{",
            "  let l = [];",
            "  for (var vx = 1, vy = 2, vz = 3; vx < 10; vx++) {",
            "    let lx = vx, ly = vy, lz = vz;",
            "    l.push(function() { return [ lx, ly, lz ]; });",
            "  }",
            "}"),
        lines(
            "{",
            "  var l = [];",
            "  var $jscomp$loop$0 = {};",
            "  var vx = 1, vy = 2, vz = 3;",
            "  for (; vx < 10; $jscomp$loop$0 = {lx: $jscomp$loop$0.lx,",
            "      ly: $jscomp$loop$0.ly, lz: $jscomp$loop$0.lz}, vx++){",
            "    $jscomp$loop$0.lx = vx;",
            "    $jscomp$loop$0.ly = vy;",
            "    $jscomp$loop$0.lz = vz;",
            "    l.push(function($jscomp$loop$0) {",
            "        return function() {",
            "            return [ $jscomp$loop$0.lx, $jscomp$loop$0.ly, $jscomp$loop$0.lz ];",
            "        };",
            "    }($jscomp$loop$0));",
            "  }",
            "}"));
  }

  public void testBlockScopedFunctionDeclaration() {
    test(
        lines(
            "function f() {",
            "  var x = 1;",
            "  if (a) {",
            "    function x() { return x; }",
            "  }",
            "  return x;",
            "}"),
        lines(
            "function f() {",
            "  var x = 1;",
            "  if (a) {",
            "    var x$0 = function() { return x$0; };",
            "  }",
            "  return x;",
            "}"));
  }


  public void testClass() {
    test(lines(
            "class C {}",
            "var c1 = C;",
            "{",
            "  class C {}",
            "  var c2 = C;",
            "}",
            "C === c1;"),
        lines(
            "class C {}",
            "var c1 = C;",
            "{",
            "  class C$0 {}",
            "  var c2 = C$0;",
            "}",
            "C === c1;"));
  }

  public void testRenameJsDoc() {
    test(lines(
        "function f() {",
        "  let Foo = 4;",
        "  if (Foo > 2) {",
        "    /** @constructor */",
        "    let Foo = function(){};",
        "    let /** Foo */ f = new Foo;",
        "    return f;",
        "  }",
        "}"),
        lines(
        "function f() {",
        "  var Foo = 4;",
        "  if (Foo > 2) {",
        "    /** @constructor */",
        "    var Foo$0 = function(){};",
        "    var /** Foo$0 */ f$1 = new Foo$0;",
        "    return f$1;",
        "  }",
        "}"));

    test(lines(
        "function f() {",
        "  let Foo = 4;",
        "  if (Foo > 2) {",
        "    /** @constructor */",
        "    let Foo = function(){};",
        "    let f = /** @param {Foo} p1 @return {Foo} */ function(p1) {",
        "        return new Foo;",
        "    };",
        "    return f;",
        "  }",
        "}"),
        lines(
        "function f() {",
        "  var Foo = 4;",
        "  if (Foo > 2) {",
        "    /** @constructor */",
        "    var Foo$0 = function(){};",
        "    var f$1 = /** @param {Foo$0} p1 @return {Foo$0} */ function(p1) {",
        "        return new Foo$0;",
        "    };",
        "    return f$1;",
        "  }",
        "}"));

    test(lines(
        "function f() {",
        "  let Foo = 4;",
        "  let Bar = 5;",
        "  if (Foo > 2) {",
        "    /** @constructor */ let Foo = function(){};",
        "    /** @constructor */ let Bar = function(){};",
        "    let /** Foo | Bar */ f = new Foo;",
        "    return f;",
        "  }",
        "}"),
        lines(
        "function f() {",
        "  var Foo = 4;",
        "  var Bar = 5;",
        "  if (Foo > 2) {",
        "    /** @constructor */ var Foo$0 = function(){};",
        "    /** @constructor */ var Bar$1 = function(){};",
        "    var /** Foo$0 | Bar$1 */ f$2 = new Foo$0;",
        "    return f$2;",
        "  }",
        "}"));
  }

  public void testCatch() {
    test("function f(e) { try {} catch (e) { throw e; } }",
         "function f(e) { try {} catch (e$0) { throw e$0; } }");

    test(lines(
        "function f(e) {",
        "  try {",
        "    function f(e) {",
        "      try {} catch (e) { e++; }",
        "    }",
        "  } catch (e) { e--; }",
        "}"),
        lines(
        "function f(e) {",
        "  try {",
        "    var f$1 = function(e) {",
        "      try {} catch (e$0) { e$0++; }",
        "    }",
        "  } catch (e$2) { e$2--; }",
        "}"));
  }

  public void testBlockScopedGeneratorFunction() {
    // Functions defined in a block get translated to a var
    test(
        "{ function *f() {yield 1;} }",
        "{ var f = function*() { yield 1; }; }");
  }

  public void testExterns() {
    testExternChanges("let x;", "", "var x;");
  }
}
