# HwFrameWorkSource
## 本项目为华为 framework 源码合集：
    - Mate10 EMUI 5.1 8.1.0
    - P9 EMUI 5.0 8.0.0
    - Mate20 EMUI 9.0.0 9.0.0

## framework 提取步骤：

* 1. 拷贝 framework 到本地目录：

```bash
    adb pull /system/framework .
```

* 2. 利用 [vdexExtractor](https://github.com/anestisb/vdexExtractor)工具将 *.vdex 转化为 dex 文件

```bash
vdexExtractor -i input_file -o . output_file
```

* 3. 利用 [jadx](https://github.com/skylot/jadx)将转化后的 dex 文件转化为 java 源码;

* 4. 编写脚本 convert.sh：

```bash
    files=`find arm64 oat/arm64 -name "*.vdex" -o -name "*.ovdex"`
    if [ -d "tmp" ]; then
        echo 文件夹存在
        rm -rf tmp
    fi
    mkdir tmp
    echo $PWD
    prefix=$PWD
    cd tmp
    echo "==========从oat或odex中提取dex================="
    for file in $files
    do
        vdexExtractor -i "$prefix/$file" -o .
    done
    echo "===========dex转java=============="
    files=`find . -name "*.dex"`
    for file in $files
    do
        jadx -d code --show-bad-code $file
    done
```
* 5. Android 9.0 dex2oat 生成的衍生文件（odex、vdex 和 cdex）, 其中 cdex 需要用到 [compact_dex_converter](https://github.com/anestisb/vdexExtractor/blob/master/tools/deodex/run.sh) 脚本下载 compact_dex_converter 工具转化为标准的 dex，再用 最新的 jadx 转化成 java 源码  

* 6. 将脚本放到 framework 目录下修改权限，执行即可

参考链接：<https://github.com/dstmath/HWFramework>