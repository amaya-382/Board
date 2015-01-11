$(function () {
    $(".delete").click(function () {
        $.post(
            "/board/delete",
            { "id": $(this).parent().attr("id"), "token": $("#token").val()},
            function (res) {
                $("#" + res).remove();
            });
    });
});
