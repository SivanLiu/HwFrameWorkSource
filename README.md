# HwFrameWorkSource
## 本项目为华为 framework 源码合集：
    
    Mate10 EMUI 5.1 8.1.0
    P9 EMUI 5.0 8.0.0
    Mate20 EMUI 9.0.0 9.0.0

## framework 提取步骤：

### 1. 拷贝 framework 到本地目录：

```bash
    adb pull /system/framework .
```

### 2. 利用 [vdexExtractor](https://github.com/anestisb/vdexExtractor)工具将 *.vdex 转化为 dex 文件

```bash
    vdexExtractor -i input_file -o . output_file
```

### 3. 利用 [jadx](https://github.com/skylot/jadx)将转化后的 dex 文件转化为 java 源码;

### 4. 编写脚本 convert.sh，请见 [convert.sh](https://github.com/SivanLiu/HwFrameWorkSource/blob/master/convert.sh)

### 5. 注意：
- Android 9.0 dex2oat 生成的衍生文件（odex、vdex 和 cdex）, 其中 cdex 需要用到 [compact_dex_converter](https://github.com/anestisb/vdexExtractor/blob/master/tools/deodex/run.sh) 
- compact_dex_converter 工具可以将 cdex 转化为标准的 dex，再用最新的 jadx 转化成 java 源码，旧版本的 jadx 转化可能有问题，建议自己编译最新的源码使用； 
- run.sh 会去 one.drive下载 compact_dex_converter，速度太慢，自己可以先根据 [constants.sh](https://github.com/anestisb/vdexExtractor/blob/master/tools/deodex/constants.sh) 中的网址下载解压后放到对应的目录：
[linux](https://onedrive.live.com/download?cid=D1FAC8CC6BE2C2B0&resid=D1FAC8CC6BE2C2B0%21581&authkey=AE_kzPqzG_-R4T0)、[mac](https://onedrive.live.com/download?cid=D1FAC8CC6BE2C2B0&resid=D1FAC8CC6BE2C2B0%21580&authkey=ADMmFqIo6bj7X5Y)
    
    其中 linux 路径:
 ```groovy
 vdexExtractor/tools/deodex/hostTools/Linux/api-API_28
```

- 将 run.sh 中 deps_prepare_env "$apiLevel" 一行注释掉就会使用本地已经下载好的依赖

### 6. 将脚本放到 framework 目录下修改权限，执行即可

参考链接：<https://github.com/dstmath/HWFramework>
