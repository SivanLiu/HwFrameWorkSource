files=`find arm64 oat/arm64 -name "*.vdex" -o -name "*.ovdex"`
if [ -d "tmp" ]; then
    echo 文件夹存在
    rm -rf tmp
fi
mkdir tmp
echo $PWD
prefix=$PWD
cd tmp
echo "============从 oat 或 odex 中提取 dex==========="
for file in $files
do
	echo $file
	vdexExtractor -i "$prefix/$file" -o .
done

echo "===============将 cdex 转为标准的 dex==========="
cdex_files=`find . -name "*.cdex"`
for cdex_file in $cdex_files
do
	echo $cdex_file
	fileName=$(basename "$cdex_file" .cdex)
	compact_dex_converter -w . $cdex_file
done

echo "===============重命名 cdex 转换文件名=========="
cdex_out_files=`find . -name "*.cdex"`
for cdex_out_file in $cdex_out_files
do 
	echo $cdex_out_file
	fileName=$(basename "$cdex_out_file" .cdex)
	mv "$cdex_out_file" "$fileName.dex"
done

echo "===================dex 转 java==================="

files=`find . -name "*.dex"`
for file in $files
do
	jadx -d code --show-bad-code $file
done
