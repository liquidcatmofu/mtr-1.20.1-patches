# MTR 1.20.1 Patches

[English](README.md)

Minecraft Transit Railway 4.0.5 / Forge 1.20.1 向けの小規模なクライアントサイドパッチ集です。

このプロジェクトは MTR 本体とは意図的に分離されています。Minecraft 1.20.1 環境を維持しつつ、範囲を限定した互換性・メモリ・パフォーマンス修正を適用したい環境を対象としています。

## 現在のパッチ

### 非表示の半透明バッチのリーク

MTR で半透明パーツを非表示に設定すると、`BatchManager#drawAll` は半透明バッチの描画をスキップします。影響を受ける 1.20.1 のコードパスでは、その際にバッチの `clear()` も実行されないため、`RenderCall` エントリが毎フレーム蓄積し続けます。

このパッチは、半透明描画が無効な場合に `drawAll` の終了時点で半透明バッチをクリアします。

パッチ適用前 / 設定による回避前の観測例:

- キューに追加された render call: 253,329,046
- `drawBatch` 経由で確認された clear: 252,455,155
- 約 21 分後に未処理として残った queued call: 約 873,891

半透明描画を有効にした場合は、queued 数と観測された cleared 数が完全に一致しました。

### CachedResource の strong registry によるリーク

MTR の `CachedResource` は、`tick()` からキャッシュデータを期限切れにできるよう、すべてのインスタンスを static な `CACHED_RESOURCES` コレクションに保存します。ネストされたキャッシュは繰り返し生成される場合があるため、この static コレクション自体が不要になった `CachedResource` インスタンスや supplier closure を強参照し続ける可能性があります。

このパッチは、新しく生成された resource を MTR の strong registry から外し、代わりに `WeakReference` で追跡します。まだ生存している resource に対しては元の expiry 挙動を再現します。reflection hook の初期化に失敗した場合は、MTR 本来の挙動を変更せず、エラーを 1 回だけログに出力します。

### CachedResource の access と expiry の結合（実験的）

MTR はキャッシュ再構築を抑制するために、1 つの static な `canFetchCache` フラグを使用しています。元の `getData` では、この同じフラグが `expiry = now + lifespan` の更新も制御しています。別のキャッシュ再構築によって global flag が false になると、他のキャッシュが正常に読み出されても idle lifetime が延長されない場合があります。

このパッチは、global rebuild throttle が閉じている場合でも、正常にアクセスされたキャッシュの `expiry` を更新します。ただし、以前の expiry がまだ切れていない値だけを延長するため、すでに期限切れになった entry を復活させることはありません。再構築スケジュール自体は変更しません。

runtime profiling では、通常プレイ中に suppressed refresh が発生し、TTL の残り時間が数秒まで短くなる access も確認できました。一方で、それが一貫して高コストな expiry / rebuild を引き起こすところまでは確認できませんでした。このため、このパッチは実験的な選択肢として残し、デフォルトでは無効にしています。

### Lift model の再構築 churn

`RenderLifts#render` は、表示中の lift を描画するたびに新しい `ModelLift1` を生成します。`ModelLift1` は constructor 内で vanilla の `ModelPart` tree を構築・bake します。20 分 06 秒の profiler capture では、`ModelLift1` が 46,504 回 build され、4,231,864 個の `ModelPart` を生成していました（1 build あたり正確に 91 個）。これは、その capture で観測されたすべての `ModelPart` 構築の約 78% に相当します。

このパッチは、その constructor 呼び出しを `(height, width, depth, isDoubleSided)` を key とする小さなキャッシュへ redirect します。同じ geometry の lift は同じ bake 済み model を再利用します。キャッシュは access-order の LRU で、上限を 64 entries に設定しているため、特殊な lift dimension の組み合わせによって model が無制限に保持されることはありません。

runtime validation では期待どおりの挙動を確認しています。reset 後、すでに一度確認済みの lift では新しい `ModelLift1` build は発生せず、異なる size の lift を訪れた場合も、毎フレーム再構築されるのではなく、新しい 91-part model が 1 回だけ build されました。

Mixin はコンパイラ生成の lambda method 名には依存せず、`RenderLifts` 内の正確な `ModelLift1(int, int, int, boolean)` constructor を対象にします。

### StoredModelResource の二重 fetch

`StoredModelResourceBase#render` は renderer path を選択する前に、optimized model と fallback の `DynamicVehicleModel` の両方を取得します。対象 MTR build では optimized rendering が使用されるにもかかわらず、使用されない fallback getter も同じ基礎 `CachedResource` へ到達します。

このパッチは、使用されない renderer path の getter 呼び出しを省略します。実際に選択される path の model 構築 semantics は変更しません。

runtime validation では、optimized rendering 有効時に fallback の dynamic-model getter が 0 回になることを確認しました。計測した rail render workload では、optimized rail getter 1 回あたりに正規化した `CachedResource#getData` 呼び出しが約 2.064 から 1.155 へ減少し、約 44% の削減となりました。

## 設定

Forge の client config は `config/mtr_patches-client.toml` に生成されます。

- `fixHiddenTranslucentBatchLeak = true`
- `fixCachedResourceRegistryLeak = true`
- `fixCachedResourceAccessExpiry = false`（実験的）
- `fixLiftModelRebuild = true`
- `fixStoredModelResourceDoubleFetch = true`

設定を変更した後はクライアントを再起動してください。

## 対象環境

- Minecraft 1.20.1
- Forge 47.x
- MTR 4.0.5
- Java 17

MTR dependency は runtime validation を実施したバージョンへ意図的に固定しています。この MOD の Mixins は MTR の実装詳細を patch するため、個別の検証を行うまでは他の MTR 4.0.x build を互換対象として宣言しません。

## テスト

長時間セッションでの挙動を比較するには、別プロジェクトの `mtr-profiler` 診断 MOD を使用できます。特に以下を確認します。

- `BatchManager.RenderCall` の queued / observed-cleared / unaccounted counts
- `CachedResource` の生成 rate
- GC 後の `ModelPart` live count / heap
- 通常の 60 秒 model lifetime を超える期間で繰り返される `VehicleModel` / `DynamicVehicleModel` rebuild
- `EntityModelExtension buildModel attribution`; lift patch 有効時は、`ModelLift1` build はおおむね distinct cached lift geometry ごとに 1 回になるはずです
- StoredModelResource の二重 fetch patch を検証する場合の `RailResource` optimized / dynamic getter counts

profiler とこの patch MOD は同時に導入できます。

## ビルド

このプロジェクトでは、MTR 本体を bundle せずに redirect 先へ正確な MTR constructor type を指定するため、compile-only の小さな `ModelLift1` signature stub を使用しています。stub source set は出力 jar には含まれず、実際の class は runtime に MTR から提供されます。

## ライセンス

MIT
