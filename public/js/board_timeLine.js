$(function () {
    $(".delete").click(function () {
        $.post(
            "/board/delete",
            { "id": $(this).parent().attr("id"), "token": $("#token").val()},
            function (res) {
                $("#" + res).remove();
            });
    });

    var $time_line = $("#content");
    setTimeout(function () {
        $time_line.scrollTop($time_line[0].scrollHeight);
    }, 100);
});
