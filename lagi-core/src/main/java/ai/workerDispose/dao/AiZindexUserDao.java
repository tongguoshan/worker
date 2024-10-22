package ai.workerDispose.dao;

import ai.database.impl.MysqlAdapter;
import ai.workerDispose.pojo.NodeValue;
import ai.workerDispose.pojo.WeightObj;

import java.util.ArrayList;
import java.util.List;

public class AiZindexUserDao extends MysqlAdapter {
    /**
     * 查询
     * @return
     */
    public List<NodeValue> getNodeValue(int pageNumber, int pageSize) {
        int pageNumbers =(pageNumber - 1) * pageSize;
        String sql = "SELECT azu.did,aud.plain_text AS plainText,azu.nid,aun.comments AS node,azu.weight " +
                " FROM (SELECT * FROM ai_zindex_user ORDER BY did LIMIT ?,?) azu \n" +
                " LEFT JOIN ai_unindex_dict aud ON aud.did = azu.did \n" +
                " LEFT JOIN ai_unindex_node aun ON aun.nid = azu.nid;";
        List<NodeValue> list = select(NodeValue.class, sql,pageNumbers, pageSize);
        return list.size() > 0 && list != null ? list : new ArrayList<>();
    }

    /**
     * 查询指数所有的类型
     * @return
     */
    public boolean updateWeight(Integer nid ,Integer did , Double weight) {
        int count =-1;
        Integer ret = -1;
        String sql1 = "SELECT COUNT(*) FROM ai_zindex_user1 WHERE nid = ? AND did = ? ";
        count = selectCount(sql1, nid, did);
        if (count > 0){
            String sql = "UPDATE ai_zindex_user1 " +
                    "SET weight = ? WHERE did = ? AND nid = ?;";
            ret =executeUpdate(sql,weight, did ,nid);
        }else {
            String sql = "INSERT INTO ai_zindex_user1(nid,did,weight) VALUES(?,?,?)";
            ret =executeUpdate(sql,nid, did ,weight);
        }

        return ret > 0;
    }
    /**
     * 查询权重
     * @return
     */
    public WeightObj selectWeight(Integer nid , Integer did) {
        String sql = "SELECT * FROM ai_zindex_user WHERE nid = ? AND did = ? ";
        List<WeightObj> list = select(WeightObj.class, sql,nid, did);
        if (list.size()> 0 && list != null){
            return list.get(0);
        }else {
            WeightObj weightObj = new WeightObj();
            weightObj.setDid(did);
            weightObj.setNid(nid);
            weightObj.setWeight(0.0);
            return weightObj;
        }
    }
}
