<!DOCTYPE html>
<html xmlns="http://www.w3.org/1999/xhtml">
<head lang="en">
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"></meta>
    <title>点歌</title>
    <style>
        html, body {
            height: 100%;
            margin: 0;
            padding: 0;
        }
        .container {
            background: rgba(0, 0, 0, 0.2) url('${bg}') center center no-repeat; background-size: cover;
            width: ${width?replace(",", "")}px;
            height: ${height?replace(",", "")}px;
            padding: 10px;
        }
        table {
            width: ${width?replace(",", "")}px;
            table-layout: fixed;
            border-collapse: collapse;
            box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
            background: linear-gradient(45deg, #a37d7d, #13bfb7);
            opacity: 0.88;
        }
        th {
            text-align: center;
            font-weight: bold;
            padding: 5px;
            border: 1px solid black;
            font-size: 16px;
        }
        tr:nth-child(even) {
            background-color: #f2f2f2;
        }
        td {
            padding: 5px;
            border: 1px solid black;
            list-style: disc inside;
            min-width: 60px;
            font-size: 14px;
        }
        tr {
            height: 32px;
        }
    </style>
</head>
<body>
<div class="container">
    <table>
        <caption style="font-size: 24px; height: 40px; line-height: 40px;">歌曲列表</caption>
        <tr>
            <th colspan="1">序号</th>
            <th colspan="2">歌手</th>
            <th colspan="5">歌名</th>
        </tr>
        <#list list as item>
            <tr>
                <td colspan="1" style="text-align: center">${item_index}</td>
                <#if item.singername??>
                    <td colspan="2" style="white-space: nowrap; text-overflow:ellipsis; overflow: hidden;">${item.singername}</td>
                <#else>
                    <td colspan="2" style="white-space: nowrap; text-overflow:ellipsis; overflow: hidden;">${item.singer}</td>
                </#if>
                <#if item.name??>
                    <td colspan="5" style="white-space: nowrap; text-overflow:ellipsis; overflow: hidden;">${item.name}</td>
                <#else>
                    <td colspan="5" style="white-space: nowrap; text-overflow:ellipsis; overflow: hidden;">${item.song}</td>
                </#if>
            </tr>
        </#list>
        <tr>
            <td colspan="8" style="font-size: 18px; line-height: 24px; color: red; text-align: center; font-weight: bold;">使用 #序号 听歌， 使用 p数字 翻页</td>
        </tr>
    </table>
</div>
</body>
</html>
