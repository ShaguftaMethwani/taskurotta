
describe('taskurotta interrupted task groups', function() {

    it('should list tasks', function() {
        browser.get('http://tsk_http/app.html');
        element(by.linkText("Tasks2")).click();
        element(by.linkText("Interrupted tasks groups...")).click();
    });
});

